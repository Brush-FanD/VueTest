package com.hkd.service.impl;

import com.hkd.model.User;
import com.hkd.service.AiService;
import com.hkd.service.AuthService;
import com.hkd.exception.AiServiceException;
import com.hkd.exception.BusinessException;
import com.hkd.session.SessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    /** 单条提问的最大长度，防止超长输入把上下文 / 费用打爆 */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final AuthService authService;

    public AiServiceImpl(@Qualifier("carChatClient") ChatClient chatClient,
                         ChatMemory chatMemory,
                         AuthService authService) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.authService = authService;
    }

    @Override
    public void clearMemory() {
        String conversationId = conversationId();
        chatMemory.clear(conversationId);
        log.info("已清空会话记忆，conversationId={}", conversationId);
    }

    @Override
    public String chat(String message) {
        String content = validate(message);
        try {
            String reply = chatClient.prompt()
                    .user(content)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId()))
                    .call()
                    .content();
            return reply == null ? "" : reply;
        } catch (Exception e) {
            // 原始异常可能包含上游详细信息，只记日志，不回传给前端
            log.error("AI 同步对话失败，conversationId={}", conversationId(), e);
            throw new AiServiceException("AI 服务暂时不可用，请稍后重试", e);
        }
    }

    @Override
    public Flux<String> chatStream(String message) {
        String content = validate(message);
        String conversationId = conversationId();
        // Flux 是惰性的，异常会在订阅（真正请求模型）时才抛出，所以错误处理要用 onErrorResume
        return chatClient.prompt()
                .user(content)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("AI 流式对话失败，conversationId={}", conversationId, e);
                    return Flux.just("抱歉，AI 服务暂时不可用，请稍后重试");
                });
    }

    /**
     * 记忆 key：登录用户按 userId 隔离，未登录按匿名会话隔离。
     * 两者都由服务端决定，前端既读不到也传不进来，因此不可能靠猜 / 造 ID 去读写别人的记忆。
     */
    private String conversationId() {
        User user = authService.currentUser();
        if (user != null) {
            return "user-" + user.getId();
        }
        return "anon-" + SessionContext.require();
    }

    private String validate(String message) {
        String content = message == null ? "" : message.trim();
        if (content.isEmpty()) {
            throw new BusinessException("消息不能为空");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException("消息不能超过 " + MAX_MESSAGE_LENGTH + " 个字");
        }
        return content;
    }
}