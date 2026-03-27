package com.rs.doanmonhoc.dto;

public record LeaveTypeResponse(
        Integer id,
        String name,
        Integer maxDaysPerYear,
        boolean paid,
        Double payPercentage) {}
