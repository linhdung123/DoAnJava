package com.rs.doanmonhoc.dto;

import java.util.Set;

public record AuthLoginResponse(
        String accessToken,
        String tokenType,
        Integer employeeId,
        String employeeCode,
        String fullName,
        Integer departmentId,
        Set<String> roles,
        boolean mustChangePassword) {}
