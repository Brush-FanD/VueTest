package com.hkd.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存向量库：暴力余弦相似度检索 + JSON 落盘持久化。
 *
 * <p>检索用暴力遍历，几千个片段毫秒级；数据量大时换 PGVector / Milvus，
 * 只要保持 add / search / documents 这几个方法不变即可。</p>
 *
 * <p>持久化：正文存文本、向量用 Base64(float32) 存，启动时加载，
 * 入库 / 删除 / 清空时原子写回（先写 .tmp 再 rename）。</p>
 */
@Slf4j
@Component
public class InMemoryVectorStore {

    public record Entry(String id, String docId, String title, String source, String text, float[] vector) {
    }

    public record Hit(String id, String docId, String title, String source, String text, double score) {
    }

    /** 落盘用的扁平结构（向量编码成字符串，JSON 更紧凑） */
    public record StoredEntry(String id, String docId, String title, String source, String text, String vector) {
    }

    public record Snapshot(int version, List<StoredEntry> entries) {
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;
    private final RagProperties properties;

    public InMemoryVectorStore(ObjectMapper objectMapper, RagProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    public void load() {
        Path path = storePath();
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            Snapshot snapshot = objectMapper.readValue(path.toFile(), Snapshot.class);
            List<Entry> loaded = new ArrayList<>();
            if (snapshot != null && snapshot.entries() != null) {
                for (StoredEntry stored : snapshot.entries()) {
                    loaded.add(new Entry(stored.id(), stored.docId(), stored.title(), stored.source(),
                            stored.text(), decode(stored.vector())));
                }
            }
            entries.clear();
            entries.addAll(loaded);
            log.info("已加载知识库索引：{} 个片段（{}）", entries.size(), path);
        } catch (Exception e) {
            log.warn("加载知识库索引失败，将以空库启动：{}", path, e);
        }
    }

    public void add(List<Entry> newEntries) {
        if (newEntries != null && !newEntries.isEmpty()) {
            entries.addAll(newEntries);
            save();
        }
    }

    /** 检索与 query 最相似的 topK 个片段，低于 threshold 的直接丢弃 */
    public List<Hit> search(float[] query, int topK, double threshold) {
        List<Hit> hits = new ArrayList<>();
        for (Entry entry : entries) {
            double score = cosine(query, entry.vector());
            if (score >= threshold) {
                hits.add(new Hit(entry.id(), entry.docId(), entry.title(), entry.source(), entry.text(), round(score)));
            }
        }
        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        return hits.size() > topK ? new ArrayList<>(hits.subList(0, topK)) : hits;
    }

    /** 按文档聚合，供前端展示已入库的资料列表 */
    public List<Map<String, Object>> documents() {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Entry entry : entries) {
            Map<String, Object> doc = grouped.computeIfAbsent(entry.docId(), key -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("docId", entry.docId());
                map.put("title", entry.title());
                map.put("source", entry.source());
                map.put("chunks", 0);
                return map;
            });
            doc.put("chunks", ((int) doc.get("chunks")) + 1);
        }
        return new ArrayList<>(grouped.values());
    }

    public int chunkCount() {
        return entries.size();
    }

    public void removeDocument(String docId) {
        if (docId == null || docId.isBlank()) {
            return;
        }
        boolean removed = entries.removeIf(entry -> docId.equals(entry.docId()));
        if (removed) {
            save();
        }
    }

    public void clear() {
        entries.clear();
        save();
    }

    // ---------- 持久化 ----------

    private void save() {
        Path path = storePath();
        if (path == null) {
            return;
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            List<StoredEntry> stored = new ArrayList<>(entries.size());
            for (Entry entry : entries) {
                stored.add(new StoredEntry(entry.id(), entry.docId(), entry.title(), entry.source(),
                        entry.text(), encode(entry.vector())));
            }
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            objectMapper.writeValue(tmp.toFile(), new Snapshot(1, stored));
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            // 落盘失败不影响本次入库，只记录日志
            log.warn("保存知识库索引失败：{}", path, e);
        }
    }

    private Path storePath() {
        String configured = properties.getStorePath();
        if (configured == null || configured.isBlank() || "none".equalsIgnoreCase(configured)) {
            return null;
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    private static String encode(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    private static float[] decode(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[bytes.length / 4];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }

    // ---------- 数学 ----------

    private static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}