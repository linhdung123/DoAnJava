package com.rs.doanmonhoc.dto;

import com.rs.doanmonhoc.model.EmployeeStatus;
import com.rs.doanmonhoc.model.EmployeeRole;

public record EmployeeResponse(
        Integer id,
        String employeeCode,
        String fullName,
        String email,
        Integer departmentId,
        String departmentName,
        String nfcUid,
        boolean faceRegistered,
        EmployeeStatus status,
        EmployeeRole role,
        boolean mustChangePassword,
        String temporaryPassword,
        Double baseSalary,
        Double allowance,
        Integer standardWorkDays) {}
