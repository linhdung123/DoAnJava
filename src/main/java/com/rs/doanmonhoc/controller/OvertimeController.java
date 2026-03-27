package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.OvertimeApprovalRequest;
import com.rs.doanmonhoc.dto.OvertimeLogResponse;
import com.rs.doanmonhoc.security.AccessScopeService;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.service.OvertimeApprovalService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/overtime")
public class OvertimeController {

    private final OvertimeApprovalService overtimeApprovalService;
    private final AccessScopeService accessScopeService;

    public OvertimeController(
            OvertimeApprovalService overtimeApprovalService, AccessScopeService accessScopeService) {
        this.overtimeApprovalService = overtimeApprovalService;
        this.accessScopeService = accessScopeService;
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN')")
    public Map<String, Object> approve(
            @Valid @RequestBody OvertimeApprovalRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Integer resolvedEmployeeId =
                accessScopeService.resolveHistoryEmployeeId(principal, req.employeeId());
        Integer approvedByEmployeeId = principal.employeeId();
        overtimeApprovalService.setApproved(
                approvedByEmployeeId, resolvedEmployeeId, req.date(), req.approved());
        return Map.of("ok", true);
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public List<OvertimeLogResponse> logs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return overtimeApprovalService.list(principal, from, to);
    }
}

