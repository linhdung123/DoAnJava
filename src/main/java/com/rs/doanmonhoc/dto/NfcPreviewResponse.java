package com.rs.doanmonhoc.dto;

public record NfcPreviewResponse(
        Integer employeeId,
        String employeeCode,
        String fullName,
        boolean faceRegistered,
        boolean requiresLiveness) {}
