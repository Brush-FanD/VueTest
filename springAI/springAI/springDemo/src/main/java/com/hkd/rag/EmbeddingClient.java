package com.hkd.rag;

import com.hkd.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Embedding 客户端：调用 OpenAI 兼容的 /v1/embeddings 接口，自动分批。
 *
 * <p>为什么不直接用 DeepSeek：DeepSeek 只提供 chat，没有 embedding 接口，
 * 所以这里单独配一个向量化服务（默认本地 Ollama 的 bge-m3）。</p>
 */
@Slf4j
public class EmbeddingClient {

    private final RestClient restClient;
    private final RagProperties properties;

    public EmbeddingClient(RestClient restClient, RagProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public float[] embedOne(String text) {
        List<float[]> vectors = embed(List.of(text));
        if (vectors.isEmpty()) {
            throw new AiServiceException("Embedding 服务返回了空结果", null);
        }
        return vectors.get(0);
    }

    /** 分批调用：避免一篇长文一次性发几百条文本导致请求过大 / 超时 */
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        int batchSize = Math.max(1, properties.getEmbedding().getBatchSize());
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += batchSize) {
            int to = Math.min(texts.size(), from + batchSize);
            vectors.addAll(embedBatch(texts.subList(from, to)));
        }
        return vectors;
    }

    @SuppressWarnings("unchecked")
    private List<float[]> embedBatch(List<String> texts) {
        RagProperties.Embedding cfg = properties.getEmbedding();
        Map<String, Object> body = Map.of("model", cfg.getModel(), "input", texts);
        try {
            Map<String, Object> response = restClient.post()
                    .uri(cfg.getPath())
                    .headers(headers -> {
                        if (cfg.getApiKey() != null && !cfg.getApiKey().isBlank()) {
                            headers.setBearerAuth(cfg.getApiKey());
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !(response.get("data") instanceof List<?> data)) {
                throw new AiServiceException("Embedding 服务返回格式不正确", null);
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (Object item : data) {
                if (item instanceof Map<?, ?> map) {
                    items.add((Map<String, Object>) map);
                }
            }
            // 按 index 排序，保证向量与输入文本一一对应
            items.sort(Comparator.comparingInt(m -> ((Number) m.getOrDefault("index", 0)).intValue()));

            List<float[]> vectors = new ArrayList<>(items.size());
            for (Map<String, Object> item : items) {
                if (!(item.get("embedding") instanceof List<?> raw)) {
                    continue;
                }
                float[] vector = new float[raw.size()];
                for (int i = 0; i < raw.size(); i++) {
                    vector[i] = ((Number) raw.get(i)).floatValue();
                }
                vectors.add(vector);
            }
            return vectors;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 Embedding 服务失败：baseUrl={}, model={}", cfg.getBaseUrl(), cfg.getModel(), e);
            throw new AiServiceException(
                    "知识库 Embedding 服务不可用，请确认已启动（默认 Ollama: " + cfg.getBaseUrl() + "）", e);
        }
    }
}