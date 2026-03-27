package com.rs.doanmonhoc.dto;

/** Tổng hợp số ngày theo từng trạng thái trong khoảng [from, to]. */
public record MyAttendanceCalendarSummary(
        long totalDays,
        long presentDays,
        long absentDays,
        long onLeaveDays,
        long leavePendingDays) {}
