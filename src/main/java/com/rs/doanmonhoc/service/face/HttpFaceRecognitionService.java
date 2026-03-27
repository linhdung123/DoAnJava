package com.rs.doanmonhoc.service.face;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rs.doanmonhoc.exception.BusinessException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Gọi service model nhận diện (Python/ONNX/…) qua HTTP.
 * Body mặc định: {@code {"stored": "...", "probe": "..."}}
 * Phản hồi hỗ trợ: {@code {"match": true}} hoặc {@code {"score": 0.91}} / {@code {"similarity": 0.91}}.
 */
@Service
@ConditionalOnProperty(name = "app.face-recognition.provider", havingValue = "http")
public class HttpFaceRecognitionService implements FaceRecognitionService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final double threshold;

    public HttpFaceRecognitionService(
            @Value("${app.face-recognition.http.url}") String url,
            @Value("${app.face-recognition.threshold:0.82}") double threshold,
            ObjectMapper objectMapper) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Cần cấu hình app.face-recognition.http.url khi provider=http");
        }
        this.restClient = RestClient.builder().baseUrl(url).build();
        this.objectMapper = objectMapper;
        this.threshold = threshold;
    }

    @Override
    public boolean matches(String storedEmbeddingJson, String probeEmbeddingJson) {
        String body =
                restClient
                        .post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("stored", storedEmbeddingJson, "probe", probeEmbeddingJson))
                        .retrieve()
                        .body(String.class);
        if (body == null || body.isBlank()) {
            throw new BusinessException("Model khuôn mặt trả về rỗng");
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("match") && node.get("match").isBoolean()) {
                return node.get("match").asBoolean();
            }
            double score = readScore(node);
            return score >= threshold;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Không đọc được phản hồi model khuôn mặt: " + e.getMessage());
        }
    }

    private static double readScore(JsonNode node) {
        if (node.has("score") && node.get("score").isNumber()) {
            return node.get("score").asDouble();
        }
        if (node.has("similarity") && node.get("similarity").isNumber()) {
            return node.get("similarity").asDouble();
        }
        throw new BusinessException("Phản hồi model cần có match, score hoặc similarity");
    }
}
