package com.hkd.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 相关配置，全部可在 application.yml 的 app.rag 下调整。
 */
@Data
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    /** 总开关 */
    private boolean enabled = true;

    /** 最终喂给模型的资料条数（Top-K） */
    private int topK = 5;

    /** 先向量召回多少条候选，再交给重排精选（recallK >= topK 才有意义） */
    private int recallK = 20;

    /** 是否开启重排：向量相似度 + 词面匹配混合打分 */
    private boolean rerankEnabled = true;

    /** 重排时向量分的权重（其余权重给词面匹配），0~1 */
    private double rerankVectorWeight = 0.7;

    /** 相似度下限：低于它的候选在召回阶段直接丢弃，避免把不相关内容硬塞给模型 */
    private double similarityThreshold = 0.35;

    /** 每个切块的目标字数 */
    private int chunkSize = 500;

    /** 相邻切块的重叠字数：防止答案正好落在切口上被截断 */
    private int chunkOverlap = 80;

    /** 拼进 Prompt 的资料总字数上限：控制 token 成本与上下文长度 */
    private int maxContextChars = 6000;

    /** 单份资料的字数上限：防止有人贴一篇长文把服务打挂 */
    private int maxDocumentChars = 100_000;

    /** 向量索引落盘路径；设为空或 none 表示只在内存中、重启即丢 */
    private String storePath = "./data/rag-vectors.json";

    private Embedding embedding = new Embedding();

    /** OpenAI 兼容的 embedding 接口配置（默认本地 Ollama） */
    @Data
    public static class Embedding {
        private String baseUrl = "http://localhost:11434";
        private String path = "/v1/embeddings";
        private String apiKey = "ollama";
        private String model = "bge-m3";

        /** 单次请求最多带多少条文本，超出会自动分批，避免一次请求过大 */
        private int batchSize = 32;
    }
}