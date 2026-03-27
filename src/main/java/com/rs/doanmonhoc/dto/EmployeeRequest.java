package com.rs.doanmonhoc.dto;

import com.rs.doanmonhoc.model.EmployeeStatus;
import com.rs.doanmonhoc.model.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeRequest(
        @Size(max = 20) String employeeCode,
        @NotBlank @Size(max = 100) String fullName,
        @Email @Size(max = 100) String email,
        Integer departmentId,
        EmployeeStatus status,
        EmployeeRole role,
        @Size(min = 6, max = 72) String password,
        Double baseSalary,
        Double allowance,
        Integer standardWorkDays) {}
