package com.rs.doanmonhoc.dto;

import com.rs.doanmonhoc.model.AttendanceStatus;
import com.rs.doanmonhoc.model.VerifyMethod;
import java.time.Instant;
import java.time.LocalDate;

public record AttendanceLogResponse(
        Long id,
        Integer employeeId,
        /** Họ tên nhân viên (JSON: {@code fullName}) — cùng key với các API nhân sự khác. */
        String fullName,
        String employeeCode,
        Instant checkIn,
        Instant checkOut,
        LocalDate date,
        VerifyMethod verifyMethod,
        String location,
        AttendanceStatus status) {}
