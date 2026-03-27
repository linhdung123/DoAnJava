package com.rs.doanmonhoc.dto;

import com.rs.doanmonhoc.model.VerifyMethod;
import jakarta.validation.constraints.Size;

/**
 * @param nfcUid Bắt buộc khi {@code verifyMethod} là {@link VerifyMethod#NFC} hoặc null (mặc định NFC).
 * @param faceEmbeddingJson Khi {@code verifyMethod} là {@link VerifyMethod#FACE} — nhân viên lấy từ JWT, so với
 *     mẫu đã lưu.
 */
public record CheckOutRequest(
        @Size(max = 50) String nfcUid,
        String faceEmbeddingJson,
        VerifyMethod verifyMethod,
        String locationGps) {}
