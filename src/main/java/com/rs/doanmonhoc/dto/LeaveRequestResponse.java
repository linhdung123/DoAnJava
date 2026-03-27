package com.rs.doanmonhoc.dto;

import com.rs.doanmonhoc.model.LeaveRequestStatus;
import java.time.Instant;
import java.time.LocalDate;

public record LeaveRequestResponse(
        Integer id,
        Integer employeeId,
        String employeeName,
        Integer leaveTypeId,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveRequestStatus status,
        Instant createdAt) {}
