package com.hkd.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RAG 装配：embedding 客户端 + 向量库 + 专用的 RAG ChatClient。
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    /** 专门指向 embedding 服务的 RestClient（和 DeepSeek 的 chat 是两套） */
    @Bean
    public RestClient ragRestClient(RagProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getEmbedding().getBaseUrl())
                .build();
    }

    @Bean
    public EmbeddingClient embeddingClient(@Qualifier("ragRestClient") RestClient ragRestClient,
                                           RagProperties properties) {
        return new EmbeddingClient(ragRestClient, properties);
    }

    /**
     * RAG 专用 ChatClient：不带会话记忆与工具，只按检索到的资料回答，
     * 避免和历史对话串味。
     */
    @Bean("ragChatClient")
    public ChatClient ragChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}