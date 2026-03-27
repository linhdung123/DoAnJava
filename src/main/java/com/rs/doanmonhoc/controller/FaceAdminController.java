package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.FaceMatchTestRequest;
import com.rs.doanmonhoc.dto.FaceMatchTestResponse;
import com.rs.doanmonhoc.service.FaceAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller admin: cung cấp các endpoint hỗ trợ debug / kiểm tra face recognition.
 */
@RestController
@RequestMapping("/api/face")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class FaceAdminController {

    private final FaceAdminService faceAdminService;

    public FaceAdminController(FaceAdminService faceAdminService) {
        this.faceAdminService = faceAdminService;
    }

    /**
     * Test so khớp hai embedding trực tiếp.
     * Body: { "storedJson": "[...]", "probeJson": "[...]" }
     */
    @PostMapping("/test-match")
    public FaceMatchTestResponse testMatch(@Valid @RequestBody FaceMatchTestRequest req) {
        return faceAdminService.testMatch(req);
    }
}
