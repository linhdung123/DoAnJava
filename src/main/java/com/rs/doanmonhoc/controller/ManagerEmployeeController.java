package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.EmployeeResponse;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.service.EmployeeService;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/employees")
@PreAuthorize("hasAuthority('ROLE_MANAGER')")
public class ManagerEmployeeController {

    private final EmployeeService employeeService;

    public ManagerEmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeResponse> listManagedMembers(@AuthenticationPrincipal AuthPrincipal principal) {
        return employeeService.listManagedMembers(principal);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getManagedMember(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Integer id) {
        return employeeService.getManagedMember(principal, id);
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, String> resetPassword(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Integer id) {
        String newPassword = employeeService.resetEmployeePassword(principal, id);
        return Map.of("newPassword", newPassword);
    }
}
