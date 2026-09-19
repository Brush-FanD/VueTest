package com.hkd.service;

import reactor.core.publisher.Flux;

public interface AiService {
    //同步输出，传入消息和会话ID
    String chat(String message,String conversationId);

    //流式输出，传入消息和会话ID
    Flux<String> chatStream(String message, String conversationId);

    //清空指定会话的上下文记忆
    void clearMemory(String conversationId);
}
