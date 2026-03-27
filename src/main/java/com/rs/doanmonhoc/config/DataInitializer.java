package com.rs.doanmonhoc.config;

import com.rs.doanmonhoc.model.AttendanceLog;
import com.rs.doanmonhoc.model.AttendanceStatus;
import com.rs.doanmonhoc.model.Department;
import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.model.EmployeeRole;
import com.rs.doanmonhoc.model.EmployeeStatus;
import com.rs.doanmonhoc.model.LeaveRequest;
import com.rs.doanmonhoc.model.LeaveRequestStatus;
import com.rs.doanmonhoc.model.LeaveType;
import com.rs.doanmonhoc.model.OvertimeLog;
import com.rs.doanmonhoc.model.OvertimeType;
import com.rs.doanmonhoc.repository.AttendanceLogRepository;
import com.rs.doanmonhoc.repository.DepartmentRepository;
import com.rs.doanmonhoc.repository.EmployeeRepository;
import com.rs.doanmonhoc.repository.LeaveRequestRepository;
import com.rs.doanmonhoc.repository.LeaveTypeRepository;
import com.rs.doanmonhoc.repository.OvertimeLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    CommandLineRunner seed(
            DepartmentRepository departmentRepository,
            LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository,
            AttendanceLogRepository attendanceLogRepository,
            LeaveRequestRepository leaveRequestRepository,
            OvertimeLogRepository overtimeLogRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo.seed:false}") boolean seedDemo) {
        return args -> {
            if (!seedDemo) {
                return;
            }
            // Chỉ seed khi DB còn trống nghiệp vụ (cho phép đã có tài khoản admin bootstrap).
            if (departmentRepository.count() > 0 || leaveTypeRepository.count() > 0) {
                return;
            }

            Department tech = new Department();
            tech.setName("Phòng Kỹ thuật");
            tech.setDescription("Demo team kỹ thuật");
            departmentRepository.save(tech);

            Department hr = new Department();
            hr.setName("Phòng Nhân sự");
            hr.setDescription("Demo team nhân sự");
            departmentRepository.save(hr);

            LeaveType phepNam = seedLeaveType(leaveTypeRepository, "Phép năm", 12, true, 1.0);
            LeaveType nghiOm = seedLeaveType(leaveTypeRepository, "Nghỉ ốm", 30, false, 0.0);
            LeaveType viecRieng = seedLeaveType(leaveTypeRepository, "Việc riêng", 3, true, 1.0);
            seedLeaveType(leaveTypeRepository, "Nghỉ không lương", null, false, 0.0);

            Employee manager = seedEmployee(
                    employeeRepository,
                    "QL001",
                    "Trần Quản Lý",
                    "manager@company.local",
                    "04622ABB56981",
                    tech,
                    EmployeeRole.ROLE_MANAGER,
                    passwordEncoder,
                    "123456");

            Employee nv1 = seedEmployee(
                    employeeRepository,
                    "NV001",
                    "Nguyễn Văn A",
                    "a@company.local",
                    "04622ABB56980",
                    tech,
                    EmployeeRole.ROLE_EMPLOYEE,
                    passwordEncoder,
                    "123456");

            Employee nv2 = seedEmployee(
                    employeeRepository,
                    "NV002",
                    "Lê Thị B",
                    "b@company.local",
                    "04AABBCCDD11",
                    tech,
                    EmployeeRole.ROLE_EMPLOYEE,
                    passwordEncoder,
                    "123456");

            Employee nv3 = seedEmployee(
                    employeeRepository,
                    "NV003",
                    "Phạm Nhân Sự",
                    "hr@company.local",
                    "04AABBCCDD22",
                    hr,
                    EmployeeRole.ROLE_EMPLOYEE,
                    passwordEncoder,
                    "123456");

            LocalDate today = LocalDate.now(ZONE);
            LocalDate d1 = today.minusDays(1);
            LocalDate d2 = today.minusDays(2);

            // NV001: đủ công + có OT
            seedAttendance(attendanceLogRepository, nv1, d1, "08:05", "11:30", AttendanceStatus.LATE);
            seedAttendance(attendanceLogRepository, nv1, d1, "12:30", "17:10", AttendanceStatus.ON_TIME);
            seedAttendance(attendanceLogRepository, nv1, d1, "17:30", "19:00", AttendanceStatus.ON_TIME);

            // NV002: đi trễ + có ở lại tối (để test rule đi trễ không tính OT)
            seedAttendance(attendanceLogRepository, nv2, d1, "08:40", "12:00", AttendanceStatus.LATE);
            seedAttendance(attendanceLogRepository, nv2, d1, "13:00", "17:30", AttendanceStatus.ON_TIME);
            seedAttendance(attendanceLogRepository, nv2, d1, "18:00", "19:00", AttendanceStatus.ON_TIME);

            // NV003: không checkin ngày d1 để test ABSENT

            // Đơn nghỉ mẫu
            seedLeave(leaveRequestRepository, nv1, phepNam, d2, d2, "Nghỉ phép năm (demo)", LeaveRequestStatus.APPROVED);
            seedLeave(leaveRequestRepository, nv3, viecRieng, today, today, "Xin nghỉ việc riêng (demo)", LeaveRequestStatus.PENDING);
            seedLeave(leaveRequestRepository, nv2, nghiOm, today.minusDays(5), today.minusDays(5), "Nghỉ ốm (demo)", LeaveRequestStatus.APPROVED);

            // OT mẫu đã duyệt
            seedOvertime(overtimeLogRepository, nv1, d1, 1.5, manager);
            seedOvertime(overtimeLogRepository, nv1, today.minusDays(7), 2.0, manager);
            seedOvertime(overtimeLogRepository, nv1, today.minusDays(10), 1.0, manager);
            seedOvertime(overtimeLogRepository, nv2, today.minusDays(3), 0.75, manager);
            seedOvertime(overtimeLogRepository, nv2, today.minusDays(8), 1.25, manager);
            seedOvertime(overtimeLogRepository, nv3, today.minusDays(4), 1.5, manager);
            seedOvertime(overtimeLogRepository, nv3, today.minusDays(12), 2.0, manager);
        };
    }

    private static LeaveType seedLeaveType(
            LeaveTypeRepository leaveTypeRepository,
            String name,
            Integer maxDaysPerYear,
            boolean paid,
            double payPercentage) {
        LeaveType t = new LeaveType();
        t.setName(name);
        t.setMaxDaysPerYear(maxDaysPerYear);
        t.setPaid(paid);
        t.setPayPercentage(payPercentage);
        return leaveTypeRepository.save(t);
    }

    private static Employee seedEmployee(
            EmployeeRepository employeeRepository,
            String code,
            String fullName,
            String email,
            String nfcUid,
            Department department,
            EmployeeRole role,
            PasswordEncoder passwordEncoder,
            String plainPassword) {
        Employee e = new Employee();
        e.setEmployeeCode(code);
        e.setFullName(fullName);
        e.setEmail(email);
        e.setNfcUid(nfcUid);
        e.setDepartment(department);
        e.setStatus(EmployeeStatus.ACTIVE);
        e.setRole(role);
        e.setPasswordHash(passwordEncoder.encode(plainPassword));
        e.setMustChangePassword(false);
        e.setBaseSalary(role == EmployeeRole.ROLE_MANAGER ? 18000000.0 : 12000000.0);
        e.setAllowance(role == EmployeeRole.ROLE_MANAGER ? 3000000.0 : 1000000.0);
        e.setStandardWorkDays(26);
        return employeeRepository.save(e);
    }

    private static void seedAttendance(
            AttendanceLogRepository attendanceLogRepository,
            Employee employee,
            LocalDate date,
            String checkInHm,
            String checkOutHm,
            AttendanceStatus status) {
        AttendanceLog log = new AttendanceLog();
        log.setEmployee(employee);
        log.setDate(date);
        log.setCheckIn(toInstant(date, checkInHm));
        log.setCheckOut(toInstant(date, checkOutHm));
        log.setStatus(status);
        attendanceLogRepository.save(log);
    }

    private static void seedLeave(
            LeaveRequestRepository leaveRequestRepository,
            Employee employee,
            LeaveType leaveType,
            LocalDate start,
            LocalDate end,
            String reason,
            LeaveRequestStatus status) {
        LeaveRequest r = new LeaveRequest();
        r.setEmployee(employee);
        r.setLeaveType(leaveType);
        r.setStartDate(start);
        r.setEndDate(end);
        r.setReason(reason);
        r.setStatus(status);
        leaveRequestRepository.save(r);
    }

    private static void seedOvertime(
            OvertimeLogRepository overtimeLogRepository,
            Employee employee,
            LocalDate date,
            double hours,
            Employee approvedBy) {
        OvertimeLog o = new OvertimeLog();
        o.setEmployee(employee);
        o.setDate(date);
        o.setHours(hours);
        o.setType(OvertimeType.NORMAL);
        o.setApprovedBy(approvedBy);
        o.setApprovedAt(Instant.now());
        overtimeLogRepository.save(o);
    }

    private static Instant toInstant(LocalDate date, String hm) {
        String[] arr = hm.split(":");
        int h = Integer.parseInt(arr[0]);
        int m = Integer.parseInt(arr[1]);
        return LocalDateTime.of(date, LocalTime.of(h, m)).atZone(ZONE).toInstant();
    }
}
