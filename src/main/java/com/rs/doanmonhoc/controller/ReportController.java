package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.rs.doanmonhoc.security.AuthPrincipal;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Xuất bảng công tháng dạng CSV (mở được bằng Excel). */
    @GetMapping("/attendance/monthly.csv")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN')")
    public ResponseEntity<byte[]> monthlyCsv(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return reportService.monthlyAttendanceCsv(year, month, principal);
    }
}
