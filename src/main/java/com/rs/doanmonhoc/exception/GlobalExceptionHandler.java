package com.rs.doanmonhoc.exception;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /** Gọi Python/FastAPI sinh embedding lỗi mạng hoặc HTTP. */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleRestClient(RestClientException ex) {
        String hint =
                "Không gọi được service sinh embedding. Chạy FastAPI (vd. uvicorn main:app --host 0.0.0.0 --port 8000), "
                        + "đặt app.face-embedding.http.url trùng URL POST /embed, hoặc dán vector JSON khi duyệt. Chi tiết: "
                        + ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", hint));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String msg =
                ex.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}
