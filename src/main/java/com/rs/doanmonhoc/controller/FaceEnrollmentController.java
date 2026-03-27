package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.FaceEnrollmentPendingItemResponse;
import com.rs.doanmonhoc.dto.FaceEnrollmentReviewRequest;
import com.rs.doanmonhoc.dto.FaceEnrollmentStatusResponse;
import com.rs.doanmonhoc.dto.FaceEnrollmentSubmitRequest;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/face-enrollment")
public class FaceEnrollmentController {

    private final EmployeeService employeeService;

    public FaceEnrollmentController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER')")
    public FaceEnrollmentStatusResponse submit(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody FaceEnrollmentSubmitRequest req) {
        return employeeService.submitFaceEnrollment(principal, req);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER')")
    public FaceEnrollmentStatusResponse myStatus(@AuthenticationPrincipal AuthPrincipal principal) {
        return employeeService.myFaceEnrollmentStatus(principal);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<FaceEnrollmentPendingItemResponse> pending() {
        return employeeService.pendingFaceEnrollments();
    }

    @PostMapping("/{employeeId}/review")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public FaceEnrollmentStatusResponse review(
            @PathVariable Integer employeeId, @Valid @RequestBody FaceEnrollmentReviewRequest req) {
        return employeeService.reviewFaceEnrollment(employeeId, req);
    }
}
