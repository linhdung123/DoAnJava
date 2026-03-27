package com.rs.doanmonhoc.service.face;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rs.doanmonhoc.exception.BusinessException;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Gọi model service qua HTTP để lấy embedding từ ảnh.
 * Request: {"imageBase64":"..."}
 * Response hỗ trợ một trong các field: embedding | vector | faceEmbedding (array số).
 */
@Service
@ConditionalOnProperty(name = "app.face-embedding.provider", havingValue = "http")
public class HttpFaceEmbeddingExtractor implements FaceEmbeddingExtractor {

    /** POST tới URI đầy đủ (vd. http://127.0.0.1:8000/embed) — tránh lỗi ghép path với RestClient#baseUrl. */
    private final RestClient restClient = RestClient.create();

    private final URI endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpFaceEmbeddingExtractor(@Value("${app.face-embedding.http.url}") String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Cần cấu hình app.face-embedding.http.url khi provider=http");
        }
        this.endpoint = URI.create(url.trim());
    }

    @Override
    public String extractEmbeddingJson(String imageBase64) {
        String body =
                restClient
                        .post()
                        .uri(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("imageBase64", stripDataUrlIfPresent(imageBase64)))
                        .retrieve()
                        .body(String.class);
        if (body == null || body.isBlank()) {
            throw new BusinessException("Model embedding trả về rỗng");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode emb = pickEmbeddingNode(root);
            if (emb == null || !emb.isArray() || emb.isEmpty()) {
                throw new BusinessException("Phản hồi model không có mảng embedding hợp lệ");
            }
            return objectMapper.writeValueAsString(emb);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Không đọc được phản hồi embedding: " + e.getMessage());
        }
    }

    private static JsonNode pickEmbeddingNode(JsonNode root) {
        if (root.has("embedding")) return root.get("embedding");
        if (root.has("vector")) return root.get("vector");
        if (root.has("faceEmbedding")) return root.get("faceEmbedding");
        return null;
    }

    private static String stripDataUrlIfPresent(String imageBase64) {
        if (imageBase64 == null) {
            return "";
        }
        String t = imageBase64.trim();
        int comma = t.indexOf(',');
        if (t.startsWith("data:") && comma > 0) {
            return t.substring(comma + 1).trim();
        }
        return t;
    }
}
