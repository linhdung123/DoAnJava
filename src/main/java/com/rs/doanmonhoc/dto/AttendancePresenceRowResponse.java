package com.rs.doanmonhoc.dto;

import java.time.LocalDate;

/**
 * Dòng dữ liệu cho màn hình "có mặt / vắng / nghỉ" theo phạm vi (scope) quản lý:
 * mỗi nhân viên * mỗi ngày = 1 dòng.
 */
public record AttendancePresenceRowResponse(
        Integer employeeId,
        String employeeCode,
        String fullName,
        LocalDate date,
        String dayStatus, // PRESENT, ABSENT, ON_LEAVE, LEAVE_PENDING
        Long totalPresentSeconds,
        boolean shortPresent, // có check-in nhưng tổng < dailyTargetHours
        String note) {}

