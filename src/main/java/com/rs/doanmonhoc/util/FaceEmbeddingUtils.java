package com.rs.doanmonhoc.util;

import com.rs.doanmonhoc.exception.BusinessException;

public final class FaceEmbeddingUtils {

    private FaceEmbeddingUtils() {}

    public static double cosineSimilarity(String embeddingJsonA, String embeddingJsonB) {
        return cosineSimilarityVectors(parseEmbeddingArray(embeddingJsonA), parseEmbeddingArray(embeddingJsonB));
    }

    /** Cosine giữa hai vector; trả 0 nếu rỗng hoặc khác độ dài. */
    public static double cosineSimilarityVectors(float[] a, float[] b) {
        if (a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /** Chuỗi dạng JSON mảng số: "[0.1, -0.2, ...]" */
    private static float[] parseEmbeddingArray(String json) {
        if (json == null || json.isBlank()) {
            return new float[0];
        }
        String t = json.trim();
        if (t.length() < 2 || t.charAt(0) != '[' || t.charAt(t.length() - 1) != ']') {
            return new float[0];
        }
        String inner = t.substring(1, t.length() - 1).trim();
        if (inner.isEmpty()) {
            return new float[0];
        }
        String[] parts = inner.split(",");
        float[] out = new float[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                out[i] = Float.parseFloat(parts[i].trim());
            }
            return out;
        } catch (NumberFormatException e) {
            return new float[0];
        }
    }

    /**
     * Kiểm tra chuỗi JSON embedding có hợp lệ không.
     * Ném {@link BusinessException} nếu sai format hoặc sai số chiều.
     *
     * @param json         chuỗi JSON mảng số thực
     * @param expectedDim  số chiều mong đợi (0 = không kiểm tra chiều)
     */
    public static void validateEmbedding(String json, int expectedDim) {
        if (json == null || json.isBlank()) {
            throw new BusinessException("Embedding JSON không được để trống");
        }
        float[] vec = parseEmbeddingArray(json);
        if (vec.length == 0) {
            throw new BusinessException("Embedding JSON không hợp lệ – cần dạng mảng số thực, ví dụ: [0.1, -0.2, ...]");
        }
        if (expectedDim > 0 && vec.length != expectedDim) {
            throw new BusinessException(
                    "Embedding có " + vec.length + " chiều, yêu cầu " + expectedDim + " chiều");
        }
    }

    /**
     * Chuẩn hóa L2 (unit norm) một vector embedding.
     *
     * @param vec mảng float
     * @return mảng mới đã chuẩn hóa; trả về mảng gốc nếu norm = 0
     */
    public static float[] normalize(float[] vec) {
        double norm = 0;
        for (float v : vec) {
            norm += (double) v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            return vec;
        }
        float[] result = new float[vec.length];
        for (int i = 0; i < vec.length; i++) {
            result[i] = (float) (vec[i] / norm);
        }
        return result;
    }

    /**
     * Tiện ích: parse JSON → float[] để dùng bên ngoài.
     */
    public static float[] parse(String json) {
        return parseEmbeddingArray(json);
    }
}
