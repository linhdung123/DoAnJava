package com.rs.doanmonhoc.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub cho tính năng Push — tích hợp FCM/APNs ở bước sau. Hiện chỉ xác nhận đã nhận yêu cầu.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationStubController {

    @PostMapping("/enqueue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> enqueue(@RequestBody Map<String, Object> body) {
        return Map.of("status", "queued", "detail", "Chưa kết nối nhà cung cấp push thật");
    }
}
