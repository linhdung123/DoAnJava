package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Yêu cầu test so khớp hai embedding trực tiếp (dùng cho admin / debug).
 *
 * @param storedJson  embedding đã lưu trong DB (JSON mảng số thực)
 * @param probeJson   embedding từ camera / thiết bị (JSON mảng số thực)
 */
public record FaceMatchTestRequest(
        @NotBlank String storedJson,
        @NotBlank String probeJson) {}
