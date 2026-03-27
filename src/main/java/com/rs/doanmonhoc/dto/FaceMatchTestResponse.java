package com.rs.doanmonhoc.dto;

/**
 * Kết quả test so khớp face embedding.
 *
 * @param match       true nếu hai embedding khớp nhau
 * @param similarity  điểm cosine similarity [0, 1]
 * @param threshold   ngưỡng đang được cấu hình
 */
public record FaceMatchTestResponse(boolean match, double similarity, double threshold) {}
