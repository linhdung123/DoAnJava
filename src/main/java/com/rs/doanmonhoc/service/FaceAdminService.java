package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.dto.FaceMatchTestRequest;
import com.rs.doanmonhoc.dto.FaceMatchTestResponse;
import com.rs.doanmonhoc.service.face.FaceRecognitionService;
import com.rs.doanmonhoc.util.FaceEmbeddingUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service dành riêng cho admin: test so khớp face embedding trực tiếp
 * mà không cần đi qua luồng chấm công.
 */
@Service
public class FaceAdminService {

    private final FaceRecognitionService faceRecognitionService;
    private final double threshold;

    public FaceAdminService(
            FaceRecognitionService faceRecognitionService,
            @Value("${app.face-recognition.threshold:0.82}") double threshold) {
        this.faceRecognitionService = faceRecognitionService;
        this.threshold = threshold;
    }

    /**
     * Test so khớp hai embedding, trả về kết quả kèm điểm similarity và ngưỡng.
     */
    public FaceMatchTestResponse testMatch(FaceMatchTestRequest req) {
        FaceEmbeddingUtils.validateEmbedding(req.storedJson(), 0);
        FaceEmbeddingUtils.validateEmbedding(req.probeJson(), 0);

        boolean match = faceRecognitionService.matches(req.storedJson(), req.probeJson());
        double similarity = FaceEmbeddingUtils.cosineSimilarity(req.storedJson(), req.probeJson());

        return new FaceMatchTestResponse(match, Math.round(similarity * 10000.0) / 10000.0, threshold);
    }
}
