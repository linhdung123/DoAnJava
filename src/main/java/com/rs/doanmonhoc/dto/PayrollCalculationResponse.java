package com.rs.doanmonhoc.dto;

public record PayrollCalculationResponse(
        Integer employeeId,
        String employeeCode,
        String fullName,
        int month,
        int year,
        Double baseSalary,
        Integer standardWorkDays,
        Double attendedDays,
        Double paidLeaveDays,
        Double payrollDays,
        Double allowance,
        Double overtimePay,
        Double latePenalty,
        Double netSalary) {}
