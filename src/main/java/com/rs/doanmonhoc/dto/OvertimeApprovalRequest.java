package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Quản lý tick ô tăng ca để hệ thống lưu vào {@code overtime_logs}.
 *
 * Khi {@code approved=true}:
 * - Hệ thống sẽ tự tính số giờ tăng ca hợp lệ trong khung 17:00-21:00 dựa trên dữ liệu check-in/out.
 * - Nếu nhân viên đi trễ trong ngày thì không tính OT (từ chối duyệt).
 *
 * Khi {@code approved=false}: xoá OT đã duyệt trong ngày đó.
 */
public record OvertimeApprovalRequest(
        @NotNull Integer employeeId,
        @NotNull LocalDate date,
        @NotNull Boolean approved) {}

