package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotBlank;

/** Chuỗi JSON mảng số thực embedding, ví dụ: "[0.12, -0.03, ...]" */
public record FaceRegisterRequest(@NotBlank String faceEmbeddingJson) {}
