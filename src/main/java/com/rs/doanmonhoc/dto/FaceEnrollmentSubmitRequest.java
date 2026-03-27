package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotBlank;

/** Ảnh khuôn mặt do manager/employee gửi để admin duyệt. */
public record FaceEnrollmentSubmitRequest(@NotBlank String faceImageBase64) {}
