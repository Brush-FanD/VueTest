package com.hkd.service.impl;

import com.hkd.service.AiService;
import com.hkd.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    /** 前端没传会话 ID 时使用的兜底值 */
    private static final String DEFAULT_CONVERSATION_ID = "default";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public AiServiceImpl(@Qualifier("carChatClient") ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    @Override
    public void clearMemory(String conversationId) {
        chatMemory.clear(resolveConversationId(conversationId));
        log.info("已清空会话记忆，conversationId={}", conversationId);
    }

    @Override
    public String chat(String message, String conversationId) {
        try {
            String reply = chatClient.prompt()
                    .user(message)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, resolveConversationId(conversationId)))
                    .call()
                    .content();
            return reply == null ? "" : reply;
        } catch (Exception e) {
            // 原始异常可能包含上游详细信息，只记日志，不回传给前端
            log.error("AI 同步对话失败，conversationId={}", conversationId, e);
            throw new AiServiceException("AI 服务暂时不可用，请稍后重试", e);
        }
    }

    @Override
    public Flux<String> chatStream(String message, String conversationId) {
        // Flux 是惰性的，异常会在订阅（真正请求模型）时才抛出，所以错误处理要用 onErrorResume
        return chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, resolveConversationId(conversationId)))
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("AI 流式对话失败，conversationId={}", conversationId, e);
                    return Flux.just("抱歉，AI 服务暂时不可用，请稍后重试");
                });
    }

    private String resolveConversationId(String conversationId) {
        return (conversationId == null || conversationId.isBlank()) ? DEFAULT_CONVERSATION_ID : conversationId;
    }
}
