package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.LeaveRequestCreate;
import com.rs.doanmonhoc.dto.LeaveRequestResponse;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.service.LeaveRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public LeaveRequestResponse create(
            @Valid @RequestBody LeaveRequestCreate req, @AuthenticationPrincipal AuthPrincipal principal) {
        return leaveRequestService.create(req, principal.employeeId());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN')")
    public List<LeaveRequestResponse> pending(@AuthenticationPrincipal AuthPrincipal principal) {
        return leaveRequestService.listPending(principal);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public List<LeaveRequestResponse> byEmployee(
            @RequestParam(required = false) Integer employeeId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Integer scopedEmployeeId = principal.hasRole("ROLE_ADMIN") ? employeeId : principal.employeeId();
        if (scopedEmployeeId == null) {
            return List.of();
        }
        return leaveRequestService.listByEmployee(scopedEmployeeId);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN')")
    public LeaveRequestResponse approve(
            @PathVariable Integer id, @AuthenticationPrincipal AuthPrincipal principal) {
        return leaveRequestService.approve(id, principal);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN')")
    public LeaveRequestResponse reject(
            @PathVariable Integer id, @AuthenticationPrincipal AuthPrincipal principal) {
        return leaveRequestService.reject(id, principal);
    }
}
