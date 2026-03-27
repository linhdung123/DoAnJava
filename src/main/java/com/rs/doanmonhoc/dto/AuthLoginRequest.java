package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthLoginRequest(
        @NotBlank @Size(max = 20) String employeeCode,
        @NotBlank @Size(min = 6, max = 72) String password) {}
