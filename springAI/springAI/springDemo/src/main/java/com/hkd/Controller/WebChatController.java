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

    /** 流结束标记：Flux 正常结束时追加，前端收到就关闭 EventSource */
    private static final String STREAM_COMPLETE = "[COMPLETE]";

    @Autowired
    private AiService aiService;


    //同步
    @PostMapping("/aichat")
    public Result<String> aiChat(@RequestBody ChatRequest chatRequest){
        String chats = aiService.chat(chatRequest.getMessage(),
                chatRequest.getConversationId());
        return Result.success(chats);
    }


    @GetMapping(value = "/chatstream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> aiChatStream(@RequestParam String message,
                                     @RequestParam String conversationId){
        return aiService.chatStream(message, conversationId).concatWith(Flux.just(STREAM_COMPLETE));
    }

    //清空会话记忆，前端"新会话"按钮调用
    @DeleteMapping("/aichat/memory")
    public Result<Void> clearMemory(@RequestParam String conversationId){
        aiService.clearMemory(conversationId);
        return Result.success(null);
    }

}
