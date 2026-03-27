package com.rs.doanmonhoc.dto;

import java.util.List;

public record MyAttendanceCalendarResponse(
        List<MyAttendanceDayResponse> days, MyAttendanceCalendarSummary summary) {}
