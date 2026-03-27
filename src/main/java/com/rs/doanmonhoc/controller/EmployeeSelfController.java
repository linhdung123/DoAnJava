package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.EmployeeResponse;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.service.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee-self")
public class EmployeeSelfController {

    private final EmployeeService employeeService;

    public EmployeeSelfController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /** Nhân viên tự báo mất thẻ: hệ thống vô hiệu hoá thẻ bằng cách gỡ liên kết NFC UID. */
    @PostMapping("/report-lost-nfc")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public EmployeeResponse reportLostNfc(@AuthenticationPrincipal AuthPrincipal principal) {
        return employeeService.reportMyLostNfc(principal);
    }
}

