package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.AnonymousFaceCheckInRequest;
import com.rs.doanmonhoc.dto.AnonymousNfcToggleRequest;
import com.rs.doanmonhoc.dto.AttendancePresenceRowResponse;
import com.rs.doanmonhoc.dto.AttendanceLogResponse;
import com.rs.doanmonhoc.dto.CheckInRequest;
import com.rs.doanmonhoc.dto.CheckOutRequest;
import com.rs.doanmonhoc.dto.MyAttendanceCalendarResponse;
import com.rs.doanmonhoc.dto.NfcPreviewResponse;
import com.rs.doanmonhoc.security.AccessScopeService;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.service.AttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AccessScopeService accessScopeService;

    public AttendanceController(AttendanceService attendanceService, AccessScopeService accessScopeService) {
        this.attendanceService = attendanceService;
        this.accessScopeService = accessScopeService;
    }

    /** Bước 1 thiết bị: quét NFC, lấy thông tin để bật camera / liveness. */
    @GetMapping("/nfc-preview")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public NfcPreviewResponse nfcPreview(
            @RequestParam String nfcUid, @AuthenticationPrincipal AuthPrincipal principal) {
        return attendanceService.previewNfc(nfcUid, principal);
    }

    /**
     * Chấm công không cần đăng nhập: so embedding với tất cả mẫu trong DB, chọn nhân viên có cosine cao nhất (≥ ngưỡng).
     * Cảnh báo bảo mật: endpoint công khai — chỉ dùng trong mạng tin cậy / kiosk có giám sát.
     */
    @PostMapping("/check-in-by-face")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceLogResponse checkInByFace(@Valid @RequestBody AnonymousFaceCheckInRequest req) {
        return attendanceService.checkInByFaceScan(req);
    }

    /** Kiosk 1:N: lần đầu trong ngày → vào ca; đã vào → ra ca. */
    @PostMapping("/kiosk-face-toggle")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceLogResponse kioskFaceToggle(@Valid @RequestBody AnonymousFaceCheckInRequest req) {
        return attendanceService.kioskFaceToggle(req);
    }

    /** Kiosk NFC (không JWT): UID trùng DB → tự vào/ra ca. */
    @PostMapping("/kiosk-nfc-toggle")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceLogResponse kioskNfcToggle(@Valid @RequestBody AnonymousNfcToggleRequest req) {
        return attendanceService.kioskNfcToggle(req);
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public AttendanceLogResponse checkIn(
            @Valid @RequestBody CheckInRequest req, @AuthenticationPrincipal AuthPrincipal principal) {
        return attendanceService.checkIn(req, principal);
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public AttendanceLogResponse checkOut(
            @Valid @RequestBody CheckOutRequest req, @AuthenticationPrincipal AuthPrincipal principal) {
        return attendanceService.checkOut(req, principal);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public List<AttendanceLogResponse> history(
            @RequestParam(required = false) Integer employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Integer scopedEmployeeId = accessScopeService.resolveHistoryEmployeeId(principal, employeeId);
        return attendanceService.history(scopedEmployeeId, from, to);
    }

    @GetMapping("/scope-history")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public List<AttendanceLogResponse> scopeHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return attendanceService.historyByRoleScope(principal, from, to);
    }

    /**
     * Lịch cá nhân: ngày nào có mặt, vắng, nghỉ phép đã duyệt, hoặc đang chờ duyệt nghỉ — luôn theo nhân viên trong
     * token (bắt buộc đăng nhập).
     */
    @GetMapping("/my-calendar")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public MyAttendanceCalendarResponse myCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return attendanceService.myAttendanceCalendar(principal, from, to);
    }

    /**
     * Lịch có mặt / vắng / nghỉ theo phạm vi quản lý:
     * - Admin: toàn hệ thống
     * - Manager: theo phòng ban
     * - Employee/User: chỉ chính mình
     *
     * Mỗi nhân viên * mỗi ngày = 1 dòng.
     */
    @GetMapping("/scope-calendar")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    public List<AttendancePresenceRowResponse> scopeCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return attendanceService.scopePresenceCalendar(principal, from, to);
    }
}
