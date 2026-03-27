package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeaveTypeRequest(
        @NotBlank @Size(max = 50) String name,
        Integer maxDaysPerYear,
        Boolean paid,
        Double payPercentage) {}
