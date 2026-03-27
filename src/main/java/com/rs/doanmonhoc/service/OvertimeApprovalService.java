package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.config.AttendanceProperties;
import com.rs.doanmonhoc.dto.OvertimeLogResponse;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.model.AttendanceLog;
import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.model.OvertimeLog;
import com.rs.doanmonhoc.model.OvertimeType;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.repository.AttendanceLogRepository;
import com.rs.doanmonhoc.repository.EmployeeRepository;
import com.rs.doanmonhoc.repository.OvertimeLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OvertimeApprovalService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalTime OVERTIME_START = LocalTime.of(17, 0);
    private static final LocalTime OVERTIME_END = LocalTime.of(21, 0);

    private final AttendanceLogRepository attendanceLogRepository;
    private final EmployeeRepository employeeRepository;
    private final OvertimeLogRepository overtimeLogRepository;
    private final AttendanceProperties attendanceProperties;

    public OvertimeApprovalService(
            AttendanceLogRepository attendanceLogRepository,
            EmployeeRepository employeeRepository,
            OvertimeLogRepository overtimeLogRepository,
            AttendanceProperties attendanceProperties) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.employeeRepository = employeeRepository;
        this.overtimeLogRepository = overtimeLogRepository;
        this.attendanceProperties = attendanceProperties;
    }

    @Transactional
    public void setApproved(
            Integer approvedByEmployeeId, Integer employeeId, LocalDate date, Boolean approved) {
        if (employeeId == null || date == null || approved == null) {
            throw new BusinessException("Tham số không hợp lệ");
        }

        if (!approved) {
            // Xoá OT đã duyệt trước đó trong ngày (idempotent).
            List<OvertimeLog> existing =
                    overtimeLogRepository.findByEmployeeIdAndDateBetween(employeeId, date, date);
            if (!existing.isEmpty()) {
                overtimeLogRepository.deleteAll(existing);
            }
            return;
        }

        Employee emp =
                employeeRepository
                        .findById(employeeId)
                        .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));

        // Cho phép duyệt trước giờ OT: lúc duyệt chỉ lưu "đã duyệt".
        // Số giờ sẽ được tính lại theo dữ liệu thực tế trong ngày (đặc biệt sau khi checkout).
        List<AttendanceLog> dayLogs =
                attendanceLogRepository.findByEmployeeIdAndDateBetween(employeeId, date, date);
        double approvedHours = calculateApprovedHours(dayLogs);

        // Idempotent: xoá OT cũ rồi lưu OT mới.
        List<OvertimeLog> existing =
                overtimeLogRepository.findByEmployeeIdAndDateBetween(employeeId, date, date);
        if (!existing.isEmpty()) {
            overtimeLogRepository.deleteAll(existing);
        }

        OvertimeLog log = new OvertimeLog();
        log.setEmployee(emp);
        log.setDate(date);
        log.setHours(approvedHours);
        log.setType(OvertimeType.NORMAL);
        if (approvedByEmployeeId != null) {
            Employee approvedBy =
                    employeeRepository
                            .findById(approvedByEmployeeId)
                            .orElse(null);
            log.setApprovedBy(approvedBy);
        }
        log.setApprovedAt(Instant.now());
        overtimeLogRepository.save(log);
    }

    @Transactional
    public void finalizeApprovedHours(Integer employeeId, LocalDate date) {
        if (employeeId == null || date == null) {
            return;
        }
        List<OvertimeLog> existing =
                overtimeLogRepository.findByEmployeeIdAndDateBetween(employeeId, date, date);
        if (existing == null || existing.isEmpty()) {
            return; // Chưa duyệt trước thì không làm gì.
        }
        List<AttendanceLog> dayLogs =
                attendanceLogRepository.findByEmployeeIdAndDateBetween(employeeId, date, date);
        double approvedHours = calculateApprovedHours(dayLogs);
        for (OvertimeLog log : existing) {
            log.setHours(approvedHours);
        }
        overtimeLogRepository.saveAll(existing);
    }

    private boolean isLateDay(List<AttendanceLog> dayLogs) {
        LocalTime deadline =
                attendanceProperties.getExpectedStart().plusMinutes(attendanceProperties.getLateGraceMinutes());
        Instant earliest =
                dayLogs.stream()
                        .filter(l -> l.getCheckIn() != null)
                        .map(AttendanceLog::getCheckIn)
                        .min(Instant::compareTo)
                        .orElse(null);
        if (earliest == null) return false;
        LocalTime checkInTime = earliest.atZone(ZONE).toLocalTime();
        return checkInTime.isAfter(deadline);
    }

    private double calculateOvertimeHoursFromAttendance(List<AttendanceLog> dayLogs) {
        if (dayLogs == null || dayLogs.isEmpty()) {
            return 0.0;
        }
        double totalMinutes = 0.0;
        for (AttendanceLog l : dayLogs) {
            if (l.getCheckIn() == null || l.getCheckOut() == null) {
                continue; // cần check-out để xác định thời lượng
            }
            LocalTime start = l.getCheckIn().atZone(ZONE).toLocalTime();
            LocalTime end = l.getCheckOut().atZone(ZONE).toLocalTime();
            LocalTime overlapStart = start.isAfter(OVERTIME_START) ? start : OVERTIME_START;
            LocalTime overlapEnd = end.isBefore(OVERTIME_END) ? end : OVERTIME_END;
            if (!overlapEnd.isAfter(overlapStart)) {
                continue;
            }
            long mins = Duration.between(overlapStart, overlapEnd).toMinutes();
            if (mins > 0) totalMinutes += mins;
        }
        // Chỉ lấy phần 17-21 (tối đa 4h/ngày).
        totalMinutes = Math.min(totalMinutes, Duration.between(OVERTIME_START, OVERTIME_END).toMinutes());
        return totalMinutes / 60.0;
    }

    private double calculateApprovedHours(List<AttendanceLog> dayLogs) {
        if (dayLogs == null || dayLogs.isEmpty()) {
            return 0.0;
        }
        boolean lateDay = isLateDay(dayLogs);
        if (lateDay) {
            return 0.0;
        }
        return calculateOvertimeHoursFromAttendance(dayLogs);
    }

    @Transactional(readOnly = true)
    public List<OvertimeLogResponse> list(AuthPrincipal principal, LocalDate from, LocalDate to) {
        if (principal == null) {
            throw new BusinessException("Thiếu thông tin xác thực");
        }
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("Khoảng ngày không hợp lệ");
        }

        List<OvertimeLog> rows;
        if (principal.hasRole("ROLE_ADMIN")) {
            rows = overtimeLogRepository.findByDateBetweenWithEmployeeAndApprovedBy(from, to);
        } else if (principal.hasRole("ROLE_MANAGER")) {
            if (principal.departmentId() == null) {
                throw new BusinessException("Manager chưa gắn phòng ban");
            }
            rows =
                    overtimeLogRepository
                            .findByDateBetweenAndDepartmentIdWithEmployeeAndApprovedBy(
                                    from, to, principal.departmentId());
        } else {
            Integer empId = principal.employeeId();
            if (empId == null) {
                throw new BusinessException("Thiếu thông tin nhân viên");
            }
            rows = overtimeLogRepository.findByEmployeeIdAndDateBetween(empId, from, to);
        }

        return rows.stream()
                .map(
                        o -> {
                            Integer approvedById =
                                    o.getApprovedBy() != null ? o.getApprovedBy().getId() : null;
                            String approvedByCode =
                                    o.getApprovedBy() != null
                                            ? o.getApprovedBy().getEmployeeCode()
                                            : null;
                            String approvedByName =
                                    o.getApprovedBy() != null ? o.getApprovedBy().getFullName() : null;
                            String deptName =
                                    o.getEmployee().getDepartment() != null
                                            ? o.getEmployee().getDepartment().getName()
                                            : null;
                            return new OvertimeLogResponse(
                                    o.getId(),
                                    o.getEmployee().getId(),
                                    o.getEmployee().getEmployeeCode(),
                                    o.getEmployee().getFullName(),
                                    o.getDate(),
                                    o.getHours(),
                                    o.getType(),
                                    deptName,
                                    approvedById,
                                    approvedByCode,
                                    approvedByName,
                                    o.getApprovedAt());
                        })
                .sorted(
                        Comparator.comparing(OvertimeLogResponse::date)
                                .thenComparing(OvertimeLogResponse::employeeCode))
                .toList();
    }
}

