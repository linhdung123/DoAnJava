package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.PayrollCalculationResponse;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.service.PayrollService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/calculate/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public PayrollCalculationResponse calculate(
            @PathVariable Integer employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException("Thiếu thông tin đăng nhập");
        }

        if (principal.hasRole("ROLE_ADMIN")) {
            if (year != null && month != null) {
                return payrollService.calculateForMonth(
                        employeeId, java.time.YearMonth.of(year, month));
            }
            return payrollService.calculateCurrentMonth(employeeId);
        }

        if (!employeeId.equals(principal.employeeId())) {
            throw new BusinessException("Bạn chỉ được xem bảng lương của chính mình");
        }
        if (year != null && month != null) {
            return payrollService.calculateForMonth(
                    employeeId, java.time.YearMonth.of(year, month));
        }
        return payrollService.calculateCurrentMonth(employeeId);
    }
}
