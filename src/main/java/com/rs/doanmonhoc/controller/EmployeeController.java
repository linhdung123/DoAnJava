package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.EmployeeRequest;
import com.rs.doanmonhoc.dto.EmployeeResponse;
import com.rs.doanmonhoc.dto.FaceRegisterRequest;
import com.rs.doanmonhoc.dto.FaceStatusResponse;
import com.rs.doanmonhoc.dto.NfcLinkRequest;
import com.rs.doanmonhoc.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeResponse> list() {
        return employeeService.listAll();
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Integer id) {
        return employeeService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest req) {
        return employeeService.create(req);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Integer id, @Valid @RequestBody EmployeeRequest req) {
        return employeeService.update(id, req);
    }

    /** Đăng ký / cập nhật vector embedding khuôn mặt (JSON mảng số). */
    @PutMapping("/{id}/face-template")
    public EmployeeResponse registerFace(@PathVariable Integer id, @Valid @RequestBody FaceRegisterRequest req) {
        return employeeService.registerFace(id, req);
    }

    /** Kiểm tra trạng thái đăng ký khuôn mặt của nhân viên. */
    @GetMapping("/{id}/face-status")
    public FaceStatusResponse faceStatus(@PathVariable Integer id) {
        return employeeService.getFaceStatus(id);
    }

    /** Xóa (hủy) face template của nhân viên. */
    @DeleteMapping("/{id}/face-template")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFace(@PathVariable Integer id) {
        employeeService.removeFace(id);
    }

    /** Liên kết UID thẻ NFC với nhân viên. */
    @PutMapping("/{id}/nfc")
    public EmployeeResponse linkNfc(@PathVariable Integer id, @Valid @RequestBody NfcLinkRequest req) {
        return employeeService.linkNfc(id, req);
    }

    /** Admin báo mất thẻ cho nhân viên: hệ thống gỡ liên kết UID, thẻ cũ bị vô hiệu hoá. */
    @PostMapping("/{id}/report-lost-nfc")
    public EmployeeResponse reportLostNfc(@PathVariable Integer id) {
        return employeeService.reportLostNfcByAdmin(id);
    }

    /** Admin reset mật khẩu tạm cho nhân viên. */
    @PostMapping("/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable Integer id) {
        String newPassword = employeeService.resetEmployeePasswordByAdmin(id);
        return Map.of("newPassword", newPassword);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        employeeService.delete(id);
    }
}
