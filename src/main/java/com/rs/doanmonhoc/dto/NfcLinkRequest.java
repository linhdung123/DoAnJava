package com.rs.doanmonhoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NfcLinkRequest(@NotBlank @Size(max = 50) String nfcUid) {}
