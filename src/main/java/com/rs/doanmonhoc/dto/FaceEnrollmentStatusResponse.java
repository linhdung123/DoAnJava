package com.rs.doanmonhoc.dto;

import com.rs.doanmonhoc.model.FaceApprovalStatus;

public record FaceEnrollmentStatusResponse(
        Integer employeeId,
        String employeeCode,
        String fullName,
        FaceApprovalStatus status,
        String rejectReason,
        boolean faceRegistered) {}
