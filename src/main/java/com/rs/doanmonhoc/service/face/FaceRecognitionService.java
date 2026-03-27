package com.rs.doanmonhoc.service.face;

/**
 * So khớp embedding đã lưu (DB) với embedding từ camera/thiết bị.
 * Triển khai mặc định: cosine (embedding dạng JSON mảng). Có thể chuyển sang HTTP gọi service model ML.
 */
public interface FaceRecognitionService {

    boolean matches(String storedEmbeddingJson, String probeEmbeddingJson);
}
