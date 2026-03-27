package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Kiosk NFC (không JWT): dùng UID thẻ để vào/ra ca tự động. */
public record AnonymousNfcToggleRequest(
        @NotBlank @Size(max = 50) String nfcUid,
        String locationGps) {}

