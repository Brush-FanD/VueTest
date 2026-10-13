package com.hkd.controller;

import com.hkd.dto.ChatRequest;
import com.hkd.dto.Result;
import com.hkd.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/web")
public class WebChatController {

    /** 流结束标记：Flux 正常结束时追加，前端收到就结束读取 */
    private static final String STREAM_COMPLETE = "[COMPLETE]";

    @Autowired
    private AiService aiService;

    //同步：会话身份走服务端 Cookie，不再由前端传 conversationId
    @PostMapping("/aichat")
    public Result<String> aiChat(@RequestBody ChatRequest chatRequest) {
        return Result.success(aiService.chat(chatRequest.getMessage()));
    }

    @GetMapping(value = "/chatstream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> aiChatStream(@RequestParam String message) {
        return aiService.chatStream(message).concatWith(Flux.just(STREAM_COMPLETE));
    }

    //清空当前会话记忆，前端“新会话”按钮调用
    @DeleteMapping("/aichat/memory")
    public Result<Void> clearMemory() {
        aiService.clearMemory();
        return Result.success(null);
    }
}