package com.rs.doanmonhoc.dto;

import com.rs.doanmonhoc.model.VerifyMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param nfcUid Bắt buộc khi {@code verifyMethod} là {@link VerifyMethod#NFC} hoặc {@link
 *     VerifyMethod#NFC_FACE}; có thể bỏ trống khi chỉ dùng {@link VerifyMethod#FACE}.
 */
public record CheckInRequest(
        @Size(max = 50) String nfcUid,
        /** Embedding JSON từ thiết bị; bắt buộc với {@link VerifyMethod#FACE} và {@link VerifyMethod#NFC_FACE}. */
        String faceEmbeddingJson,
        @NotNull Boolean livenessPassed,
        String locationGps,
        VerifyMethod verifyMethod) {}
