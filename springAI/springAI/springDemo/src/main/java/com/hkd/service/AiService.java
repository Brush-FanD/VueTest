package com.hkd.service;

import reactor.core.publisher.Flux;

public interface AiService {
    //同步输出，传入消息
    String chat(String message);

    //流式输出，传入消息
    Flux<String> chatStream(String message);

    //清空当前会话的上下文记忆
    void clearMemory();
}