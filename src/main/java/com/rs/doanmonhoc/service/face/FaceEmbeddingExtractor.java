package com.rs.doanmonhoc.service.face;

/**
 * Sinh embedding khuôn mặt từ ảnh base64.
 */
public interface FaceEmbeddingExtractor {

    String extractEmbeddingJson(String imageBase64);
}
