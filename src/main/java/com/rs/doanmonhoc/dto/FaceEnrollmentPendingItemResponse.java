package com.rs.doanmonhoc.dto;

public record FaceEnrollmentPendingItemResponse(
        Integer employeeId,
        String employeeCode,
        String fullName,
        String faceImageBase64) {}
