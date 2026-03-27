package com.rs.doanmonhoc.dto;

/**
 * Trạng thái đăng ký khuôn mặt của một nhân viên.
 *
 * @param employeeId      ID nhân viên
 * @param employeeCode    mã nhân viên
 * @param fullName        tên nhân viên
 * @param faceRegistered  true nếu đã đăng ký face template
 */
public record FaceStatusResponse(
        Integer employeeId,
        String employeeCode,
        String fullName,
        boolean faceRegistered) {}
