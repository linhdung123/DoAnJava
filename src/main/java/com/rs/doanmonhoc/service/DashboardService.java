package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.model.AttendanceStatus;
import com.rs.doanmonhoc.model.LeaveRequestStatus;
import com.rs.doanmonhoc.dto.DashboardDayResponse;
import com.rs.doanmonhoc.repository.AttendanceLogRepository;
import com.rs.doanmonhoc.repository.LeaveRequestRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AttendanceLogRepository attendanceLogRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public DashboardService(
            AttendanceLogRepository attendanceLogRepository, LeaveRequestRepository leaveRequestRepository) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDayResponse dayOverview(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now(ZONE);
        var logs = attendanceLogRepository.findByDateWithEmployee(d);
        long checkedIn =
                logs.stream()
                        .filter(l -> l.getCheckIn() != null)
                        .map(l -> l.getEmployee().getId())
                        .distinct()
                        .count();
        long late =
                logs.stream()
                        .filter(l -> l.getCheckIn() != null && l.getStatus() == AttendanceStatus.LATE)
                        .map(l -> l.getEmployee().getId())
                        .distinct()
                        .count();
        long onLeave =
                leaveRequestRepository.findApprovedCoveringDate(LeaveRequestStatus.APPROVED, d).stream()
                        .map(r -> r.getEmployee().getId())
                        .distinct()
                        .count();
        return new DashboardDayResponse(d, checkedIn, late, onLeave);
    }
}
