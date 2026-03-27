package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LeaveRequestCreate(
        @NotNull Integer employeeId,
        @NotNull Integer leaveTypeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason) {}
