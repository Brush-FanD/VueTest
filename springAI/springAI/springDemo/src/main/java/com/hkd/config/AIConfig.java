package com.hkd.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 相关 Bean 装配：会话记忆 + 懂车AI 的 ChatClient。
 */
@Configuration
public class AIConfig {

    /** 会话记忆：按 conversationId 隔离，只保留最近 20 条消息，防止上下文无限增长 */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    /** 懂车AI：固定角色 Prompt + 会话记忆 + MCP 工具（工具服务未启动时注册为空集合） */
    @Bean("carChatClient")
    public ChatClient carChatClient(ChatClient.Builder chatClientBuilder,
                                    ChatMemory chatMemory,
                                    ToolCallbackProvider toolCallbackProvider) {
        return chatClientBuilder
                .defaultSystem("""
                        你是懂车帝的AI汽车助手"懂车AI"。
                        你的职责是：
                        1. 回答关于汽车的问题（车型对比、价格咨询、参数解析、购车建议等）
                        2. 提供车型推荐和购车建议
                        3. 解答汽车技术问题（新能源、智驾、动力系统等）
                        4. 分析汽车市场趋势
                        5. 回答汽车评测和用车相关问题

                        回答要求：
                        - 用简洁友好的中文回答，不超过150字
                        - 像懂车帝App一样专业、客观
                        - 推荐车型时说明理由
                        - 如果问题与汽车无关，礼貌告知用户专注于汽车话题
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }
}