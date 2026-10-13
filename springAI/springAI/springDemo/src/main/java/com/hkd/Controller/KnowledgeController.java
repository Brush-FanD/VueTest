package com.hkd.controller;

import com.hkd.dto.KnowledgeAskRequest;
import com.hkd.dto.KnowledgeDocumentRequest;
import com.hkd.dto.Result;
import com.hkd.rag.InMemoryVectorStore;
import com.hkd.rag.RagService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 知识库（RAG）接口。
 *
 * <p>入库 / 清空要求登录；检索与问答公开。</p>
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final RagService ragService;

    public KnowledgeController(RagService ragService) {
        this.ragService = ragService;
    }

    /** 新增资料（需登录） */
    @PostMapping("/documents")
    public Result<Map<String, Object>> add(@RequestBody KnowledgeDocumentRequest request) {
        return Result.success(ragService.ingest(request.getTitle(), request.getSource(), request.getContent()));
    }

    /** 已入库的资料列表 */
    @GetMapping("/documents")
    public Result<List<Map<String, Object>>> documents() {
        return Result.success(ragService.documents());
    }

    /** 删除单份资料（需登录） */
    @DeleteMapping("/documents/{docId}")
    public Result<Void> remove(@PathVariable("docId") String docId) {
        ragService.removeDocument(docId);
        return Result.success(null);
    }

    /** 清空知识库（需登录） */
    @DeleteMapping("/documents")
    public Result<Void> clear() {
        ragService.clear();
        return Result.success(null);
    }

    /** 只做向量召回，方便调试检索效果 */
    @GetMapping("/search")
    public Result<List<InMemoryVectorStore.Hit>> search(@RequestParam("q") String query,
                                                        @RequestParam(value = "topK", required = false) Integer topK) {
        return Result.success(ragService.search(query, topK));
    }

    /** 知识库问答：返回答案 + 引用来源 */
    @PostMapping("/ask")
    public Result<RagService.Answer> ask(@RequestBody KnowledgeAskRequest request) {
        return Result.success(ragService.answer(request.getQuestion()));
    }
}