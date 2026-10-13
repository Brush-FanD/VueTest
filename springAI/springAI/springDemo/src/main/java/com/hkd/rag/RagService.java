package com.hkd.rag;

import com.hkd.exception.AiServiceException;
import com.hkd.exception.BusinessException;
import com.hkd.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * RAG 主流程：入库（切块 → 向量化 → 存库）、检索（召回 → 重排）、问答（检索 → 拼上下文 → 作答）。
 */
@Slf4j
@Service
public class RagService {

    /** 约束模型的作答方式：只用资料、不编造、标注引用编号 */
    private static final String SYSTEM_PROMPT = """
            你是汽车知识助手，只能依据下面提供的「资料」回答问题。
            要求：
            1. 资料里有答案就据实作答，简洁、客观；
            2. 资料里没有的内容，直接回答“资料里没有提到”，绝不编造；
            3. 在回答末尾用 [1][2] 的形式标注用到的资料编号。
            """;

    private final RagProperties properties;
    private final EmbeddingClient embeddingClient;
    private final InMemoryVectorStore vectorStore;
    private final AuthService authService;
    private final ChatClient ragChatClient;

    public RagService(RagProperties properties,
                      EmbeddingClient embeddingClient,
                      InMemoryVectorStore vectorStore,
                      AuthService authService,
                      @Qualifier("ragChatClient") ChatClient ragChatClient) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.authService = authService;
        this.ragChatClient = ragChatClient;
    }

    public record Source(int index, String title, String source, String snippet, double score) {
    }

    public record Answer(String answer, List<Source> sources) {
    }

    /** 入库：需要登录 */
    public Map<String, Object> ingest(String title, String source, String content) {
        if (!properties.isEnabled()) {
            throw new BusinessException("知识库功能未启用");
        }
        authService.requireUser();
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException("资料内容不能为空");
        }
        if (text.length() > properties.getMaxDocumentChars()) {
            throw new BusinessException("资料过长，单份资料最多 " + properties.getMaxDocumentChars() + " 字");
        }
        String docTitle = (title == null || title.isBlank()) ? "未命名资料" : title.trim();
        String docSource = (source == null || source.isBlank()) ? docTitle : source.trim();

        List<String> chunks = TextChunker.split(text, properties.getChunkSize(), properties.getChunkOverlap());
        if (chunks.isEmpty()) {
            throw new BusinessException("资料内容太短，无法切分");
        }
        List<float[]> vectors = embeddingClient.embed(chunks);
        if (vectors.size() != chunks.size()) {
            throw new AiServiceException("Embedding 返回条数与切块数不一致", null);
        }

        String docId = "doc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        List<InMemoryVectorStore.Entry> entries = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            entries.add(new InMemoryVectorStore.Entry(
                    docId + "-" + i, docId, docTitle, docSource, chunks.get(i), vectors.get(i)));
        }
        vectorStore.add(entries);
        log.info("知识入库：docId={}, title={}, chunks={}", docId, docTitle, chunks.size());
        return Map.of("docId", docId, "title", docTitle, "source", docSource, "chunks", chunks.size());
    }

    /** 检索：先按向量召回 recallK 条，再重排取 topK */
    public List<InMemoryVectorStore.Hit> search(String query, Integer topK) {
        if (query == null || query.isBlank()) {
            throw new BusinessException("查询内容不能为空");
        }
        int k = (topK == null || topK <= 0) ? properties.getTopK() : Math.min(topK, 50);
        int recall = Math.max(k, properties.getRecallK());
        float[] queryVector = embeddingClient.embedOne(query.trim());
        List<InMemoryVectorStore.Hit> candidates =
                vectorStore.search(queryVector, recall, properties.getSimilarityThreshold());
        return rerank(query.trim(), candidates, k);
    }

    /**
     * 重排：向量分 + 词面（中文按字符二元组）覆盖率加权。
     *
     * <p>为什么要重排：向量擅长“语义相近”，但对型号 / 数字 / 专有名词不敏感；
     * 混入词面匹配后，问“SU7 续航”就更容易压过泛泛而谈的段落。</p>
     */
    private List<InMemoryVectorStore.Hit> rerank(String query, List<InMemoryVectorStore.Hit> candidates, int k) {
        if (candidates.size() <= k) {
            return candidates;
        }
        if (!properties.isRerankEnabled()) {
            return new ArrayList<>(candidates.subList(0, k));
        }
        double vectorWeight = Math.min(1.0, Math.max(0.0, properties.getRerankVectorWeight()));
        double lexicalWeight = 1.0 - vectorWeight;
        Set<String> queryGrams = bigrams(query);

        List<InMemoryVectorStore.Hit> scored = new ArrayList<>(candidates.size());
        for (InMemoryVectorStore.Hit hit : candidates) {
            double lexical = coverage(queryGrams, bigrams(hit.text()));
            double fused = hit.score() * vectorWeight + lexical * lexicalWeight;
            scored.add(new InMemoryVectorStore.Hit(
                    hit.id(), hit.docId(), hit.title(), hit.source(), hit.text(), round(fused)));
        }
        scored.sort(Comparator.comparingDouble(InMemoryVectorStore.Hit::score).reversed());
        return new ArrayList<>(scored.subList(0, k));
    }

    /** 问答：检索 + 生成，返回答案与引用来源 */
    public Answer answer(String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            throw new BusinessException("问题不能为空");
        }
        List<InMemoryVectorStore.Hit> hits = search(q, null);
        if (hits.isEmpty()) {
            return new Answer("知识库里没有检索到相关内容。可以换个问法，或先上传相关资料。", List.of());
        }

        String content = ragChatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("资料：\n" + buildContext(hits) + "\n\n问题：" + q)
                .call()
                .content();

        List<Source> sources = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            InMemoryVectorStore.Hit hit = hits.get(i);
            sources.add(new Source(i + 1, hit.title(), hit.source(), snippet(hit.text()), hit.score()));
        }
        return new Answer(content == null ? "" : content, sources);
    }

    public List<Map<String, Object>> documents() {
        return vectorStore.documents();
    }

    /** 删除单份资料（需登录） */
    public void removeDocument(String docId) {
        authService.requireUser();
        vectorStore.removeDocument(docId);
        log.info("已删除资料：docId={}", docId);
    }

    public void clear() {
        authService.requireUser();
        vectorStore.clear();
        log.info("知识库已清空");
    }

    public int chunkCount() {
        return vectorStore.chunkCount();
    }

    /** 拼上下文时遵守总字数上限，避免 token 爆炸 */
    private String buildContext(List<InMemoryVectorStore.Hit> hits) {
        StringBuilder sb = new StringBuilder();
        int limit = properties.getMaxContextChars();
        for (int i = 0; i < hits.size(); i++) {
            InMemoryVectorStore.Hit hit = hits.get(i);
            String block = "[资料 " + (i + 1) + " | " + hit.source() + "]\n" + hit.text() + "\n\n";
            if (sb.length() + block.length() > limit) {
                break;
            }
            sb.append(block);
        }
        return sb.toString().trim();
    }

    private String snippet(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 160 ? text : text.substring(0, 160) + "…";
    }

    /** 取字符二元组，用于中文词面匹配（不需要分词器） */
    private static Set<String> bigrams(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", "").toLowerCase();
        Set<String> grams = new HashSet<>();
        for (int i = 0; i + 1 < normalized.length(); i++) {
            grams.add(normalized.substring(i, i + 2));
        }
        return grams;
    }

    /** query 的二元组被文档覆盖的比例 */
    private static double coverage(Set<String> queryGrams, Set<String> docGrams) {
        if (queryGrams.isEmpty() || docGrams.isEmpty()) {
            return 0;
        }
        int hit = 0;
        for (String gram : queryGrams) {
            if (docGrams.contains(gram)) {
                hit++;
            }
        }
        return (double) hit / queryGrams.size();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}