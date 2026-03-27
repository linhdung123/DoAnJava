package com.rs.doanmonhoc.service.face;

import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.util.FaceEmbeddingUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.face-recognition.provider", havingValue = "cosine", matchIfMissing = true)
public class CosineFaceRecognitionService implements FaceRecognitionService {

    private final double threshold;

    public CosineFaceRecognitionService(@Value("${app.face-recognition.threshold:0.82}") double threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean matches(String storedEmbeddingJson, String probeEmbeddingJson) {
        float[] stored = FaceEmbeddingUtils.parse(storedEmbeddingJson);
        float[] probe = FaceEmbeddingUtils.parse(probeEmbeddingJson);
        if (stored.length == 0 || probe.length == 0) {
            throw new BusinessException(
                    "Không đọc được embedding — cần chuỗi JSON dạng mảng số, ví dụ [0.1, -0.2, ...]");
        }
        if (stored.length != probe.length) {
            throw new BusinessException(
                    "Số chiều embedding không khớp (mẫu đã lưu: "
                            + stored.length
                            + ", ảnh hiện tại: "
                            + probe.length
                            + "). Hãy đăng ký lại khuôn mặt trên cùng app/model FaceNet.");
        }
        double sim = FaceEmbeddingUtils.cosineSimilarityVectors(stored, probe);
        return sim >= threshold;
    }
}
