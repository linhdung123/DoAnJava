package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotNull;

/** Admin duyệt/từ chối ảnh khuôn mặt. */
public record FaceEnrollmentReviewRequest(
        @NotNull Boolean approved,
        String faceEmbeddingJson,
        String rejectReason) {}
