package com.rs.doanmonhoc.dto;

import java.time.LocalDate;

public record DashboardDayResponse(
        LocalDate date,
        long checkedInEmployees,
        long lateCount,
        long onApprovedLeaveCount) {}
