package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Chấm công vào bằng nhận diện 1:N: embedding quét được so với mọi {@code face_template} trong DB (không cần JWT).
 */
public record AnonymousFaceCheckInRequest(
        @NotBlank String faceEmbeddingJson,
        @NotNull Boolean livenessPassed,
        String locationGps) {}
