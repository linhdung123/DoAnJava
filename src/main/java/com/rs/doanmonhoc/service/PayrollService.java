package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.dto.PayrollCalculationResponse;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.model.AttendanceLog;
import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.model.LeaveRequest;
import com.rs.doanmonhoc.model.LeaveRequestStatus;
import com.rs.doanmonhoc.model.OvertimeLog;
import com.rs.doanmonhoc.model.OvertimeType;
import com.rs.doanmonhoc.config.AttendanceProperties;
import com.rs.doanmonhoc.repository.AttendanceLogRepository;
import com.rs.doanmonhoc.repository.EmployeeRepository;
import com.rs.doanmonhoc.repository.LeaveRequestRepository;
import com.rs.doanmonhoc.repository.OvertimeLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalTime OVERTIME_START = LocalTime.of(17, 0);
    private static final LocalTime OVERTIME_END = LocalTime.of(21, 0);

    private final EmployeeRepository employeeRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final AttendanceProperties attendanceProperties;
    private final LeaveRequestRepository leaveRequestRepository;
    private final OvertimeLogRepository overtimeLogRepository;
    private final OvertimeService overtimeService;
    private final WorkdayCalculatorService workdayCalculatorService;

    public PayrollService(
            EmployeeRepository employeeRepository,
            AttendanceLogRepository attendanceLogRepository,
            AttendanceProperties attendanceProperties,
            LeaveRequestRepository leaveRequestRepository,
            OvertimeLogRepository overtimeLogRepository,
            OvertimeService overtimeService,
            WorkdayCalculatorService workdayCalculatorService) {
        this.employeeRepository = employeeRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.attendanceProperties = attendanceProperties;
        this.leaveRequestRepository = leaveRequestRepository;
        this.overtimeLogRepository = overtimeLogRepository;
        this.overtimeService = overtimeService;
        this.workdayCalculatorService = workdayCalculatorService;
    }

    @Transactional(readOnly = true)
    public PayrollCalculationResponse calculateCurrentMonth(Integer employeeId) {
        return calculateForMonth(employeeId, YearMonth.now());
    }

    @Transactional(readOnly = true)
    public PayrollCalculationResponse calculateForMonth(Integer employeeId, YearMonth ym) {
        Employee employee =
                employeeRepository
                        .findByIdWithDepartment(employeeId)
                        .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên: " + employeeId));

        double baseSalary = safe(employee.getBaseSalary());
        double allowance = safe(employee.getAllowance());
        int standardWorkDays = employee.getStandardWorkDays() == null || employee.getStandardWorkDays() <= 0
                ? 26
                : employee.getStandardWorkDays();
        double dailyRate = standardWorkDays > 0 ? baseSalary / standardWorkDays : 0.0;

        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<AttendanceLog> logs = attendanceLogRepository.findByEmployeeIdAndDateBetween(employeeId, from, to);
        // Lấy toàn bộ phần trong năm để tính "còn phép" hay đã vượt maxDaysPerYear
        LocalDate yearFrom = LocalDate.of(ym.getYear(), 1, 1);
        LocalDate yearTo = LocalDate.of(ym.getYear(), 12, 31);
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeIdOverlapping(employeeId, yearFrom, yearTo);
        List<OvertimeLog> overtimeLogs = overtimeLogRepository.findByEmployeeIdAndDateBetween(employeeId, from, to);

        Map<LocalDate, List<AttendanceLog>> byDate =
                logs.stream().collect(Collectors.groupingBy(AttendanceLog::getDate));
        double attendedDays =
                byDate.values().stream()
                        .mapToDouble(
                                dayLogs -> {
                                    AttendanceLog first =
                                            dayLogs.stream()
                                                    .filter(log -> log.getCheckIn() != null)
                                                    .min(Comparator.comparing(AttendanceLog::getCheckIn))
                                                    .orElse(null);
                                    return first == null ? 0.0 : workdayByLateMinutes(first);
                                })
                        .sum();

        Set<LocalDate> paidLeaveDates = new HashSet<>();
        // Đếm quota theo từng loại nghỉ (leaveType).
        // Nếu đã dùng hết maxDaysPerYear thì phần vượt sẽ bị coi như nghỉ không lương.
        Map<Integer, List<LeaveRequest>> leavesByType =
                leaves.stream()
                        .filter(l -> l.getStatus() == LeaveRequestStatus.APPROVED)
                        .filter(l -> l.getLeaveType() != null && l.getLeaveType().getPayPercentage() != null)
                        .collect(Collectors.groupingBy(l -> l.getLeaveType().getId()));

        for (Map.Entry<Integer, List<LeaveRequest>> entry : leavesByType.entrySet()) {
            List<LeaveRequest> typeLeaves = entry.getValue();
            LeaveRequest any = typeLeaves.get(0);
            var lt = any.getLeaveType();
            if (lt == null) continue;

            double percentage = lt.getPayPercentage() != null ? lt.getPayPercentage() : 0.0;
            if (percentage <= 0.0) {
                continue; // nghỉ không lương theo cấu hình
            }

            Integer maxDaysPerYear = lt.getMaxDaysPerYear();
            if (maxDaysPerYear == null) {
                // Không giới hạn: mọi ngày trong tháng đều được coi là có lương (theo payPercentage).
                for (LeaveRequest leave : typeLeaves) {
                    for (LocalDate d = leave.getStartDate(); !d.isAfter(leave.getEndDate()); d = d.plusDays(1)) {
                        if (!d.isBefore(from) && !d.isAfter(to)) {
                            paidLeaveDates.add(d);
                        }
                    }
                }
                continue;
            }
            if (maxDaysPerYear <= 0) {
                continue;
            }

            // Gom các ngày nghỉ trong cả năm (theo leaveType) và sort để "tiêu quota" theo thứ tự thời gian.
            java.util.TreeSet<LocalDate> daySet = new java.util.TreeSet<>();
            for (LeaveRequest leave : typeLeaves) {
                for (LocalDate d = leave.getStartDate(); !d.isAfter(leave.getEndDate()); d = d.plusDays(1)) {
                    daySet.add(d);
                }
            }

            int used = 0;
            for (LocalDate d : daySet) {
                if (used >= maxDaysPerYear) {
                    break;
                }
                if (!d.isBefore(from) && !d.isAfter(to)) {
                    paidLeaveDates.add(d);
                }
                used++;
            }
        }
        double paidLeaveDays = paidLeaveDates.size();
        double payrollDays = attendedDays + paidLeaveDays;

        double hourlyRate = baseSalary / standardWorkDays / 8.0;
        double overtimePay =
                overtimeLogs.stream()
                        .mapToDouble(
                                log -> {
                                    LocalDate d = log.getDate();
                                    List<AttendanceLog> dayLogs = byDate.get(d);
                                    double candidateHours = calculateOvertimeHoursInWindow(dayLogs);
                                    boolean lateDay = isLateDay(dayLogs);
                                    double effectiveHours =
                                            lateDay ? 0.0 : Math.min(safeHours(log.getHours()), candidateHours);
                                    if (effectiveHours <= 0.0) {
                                        return 0.0;
                                    }
                                    return hourlyRate
                                            * effectiveHours
                                            * overtimeMultiplier(log.getType());
                                })
                        .sum();
        double latePenalty =
                byDate.values().stream()
                        .mapToDouble(
                                dayLogs -> {
                                    AttendanceLog first =
                                            dayLogs.stream()
                                                    .filter(log -> log.getCheckIn() != null)
                                                    .min(Comparator.comparing(AttendanceLog::getCheckIn))
                                                    .orElse(null);
                                    return first == null
                                            ? 0.0
                                            : (1.0 - workdayByLateMinutes(first)) * dailyRate;
                                })
                        .sum();

        double grossFromDays = dailyRate * payrollDays;
        double net = grossFromDays + allowance + overtimePay - latePenalty;

        return new PayrollCalculationResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                ym.getMonthValue(),
                ym.getYear(),
                round2(baseSalary),
                standardWorkDays,
                round2(attendedDays),
                round2(paidLeaveDays),
                round2(payrollDays),
                round2(allowance),
                round2(overtimePay),
                round2(latePenalty),
                round2(net));
    }

    private double workdayByLateMinutes(AttendanceLog log) {
        if (log.getCheckIn() == null || log.getDate() == null) {
            return 0.0;
        }
        LocalTime checkIn = log.getCheckIn().atZone(ZONE).toLocalTime();
        LocalTime deadline =
                attendanceProperties
                        .getExpectedStart()
                        .plusMinutes(attendanceProperties.getLateGraceMinutes());
        int lateMinutes = (int) Math.max(0, Duration.between(deadline, checkIn).toMinutes());
        return workdayCalculatorService.calculateWorkdayByLateMinutes(lateMinutes);
    }

    private double calculateOvertimeHoursInWindow(List<AttendanceLog> dayLogs) {
        if (dayLogs == null || dayLogs.isEmpty()) {
            return 0.0;
        }
        double totalMinutes = 0.0;
        for (AttendanceLog l : dayLogs) {
            if (l.getCheckIn() == null || l.getCheckOut() == null) continue;
            LocalTime start = l.getCheckIn().atZone(ZONE).toLocalTime();
            LocalTime end = l.getCheckOut().atZone(ZONE).toLocalTime();

            LocalTime overlapStart = start.isAfter(OVERTIME_START) ? start : OVERTIME_START;
            LocalTime overlapEnd = end.isBefore(OVERTIME_END) ? end : OVERTIME_END;
            if (overlapEnd.isAfter(overlapStart)) {
                totalMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
            }
        }
        double maxMinutes = Duration.between(OVERTIME_START, OVERTIME_END).toMinutes();
        return Math.min(totalMinutes, maxMinutes) / 60.0;
    }

    private boolean isLateDay(List<AttendanceLog> dayLogs) {
        if (dayLogs == null || dayLogs.isEmpty()) return false;
        Instant earliest =
                dayLogs.stream()
                        .filter(l -> l.getCheckIn() != null)
                        .map(AttendanceLog::getCheckIn)
                        .min(Instant::compareTo)
                        .orElse(null);
        if (earliest == null) return false;
        LocalTime checkInTime = earliest.atZone(ZONE).toLocalTime();
        LocalTime deadline =
                attendanceProperties
                        .getExpectedStart()
                        .plusMinutes(attendanceProperties.getLateGraceMinutes());
        return checkInTime.isAfter(deadline);
    }

    private static double safeHours(Double hours) {
        if (hours == null || hours <= 0) return 0.0;
        return hours;
    }

    private static double overtimeMultiplier(OvertimeType type) {
        if (type == null) return 1.0;
        return switch (type) {
            case NORMAL -> 1.5;
            case WEEKEND -> 2.0;
            case HOLIDAY -> 3.0;
        };
    }

    private static double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
