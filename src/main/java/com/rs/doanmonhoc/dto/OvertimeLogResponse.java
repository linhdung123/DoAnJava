package com.rs.doanmonhoc.dto;

import com.rs.doanmonhoc.model.OvertimeType;
import java.time.Instant;
import java.time.LocalDate;

public record OvertimeLogResponse(
        Long id,
        Integer employeeId,
        String employeeCode,
        String fullName,
        LocalDate date,
        Double hours,
        OvertimeType type,
        String departmentName,
        Integer approvedByEmployeeId,
        String approvedByEmployeeCode,
        String approvedByFullName,
        Instant approvedAt) {}

