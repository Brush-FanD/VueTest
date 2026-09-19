package com.hkd.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * 以 MCP 客户端身份连接 ChatMcpModel 的 SSE 服务，把远端 @Tool 方法注册成工具回调，
 * 交给 ChatClient 后大模型就能按需触发 Function Calling。
 *
 * 工具服务不可用时只打一条 WARN、不注册任何工具，主服务照常启动。
 * 先做 TCP 连通性检测再建立 SSE 连接，避免 Reactor 异步线程抛出未处理错误。
 */
@Slf4j
@Configuration
public class McpClientConfig {

    private McpSyncClient mcpSyncClient;

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            @Value("${app.mcp.server-url:http://127.0.0.1:8082}") String serverUrl,
            @Value("${app.mcp.sse-endpoint:/sse}") String sseEndpoint) {

        // 先检查端口是否可达，避免创建 Reactor Netty 客户端后异步连接失败弹出 ERROR
        if (!isReachable(serverUrl)) {
            log.warn("MCP 工具服务 {} 未启动（端口不可达），跳过工具注册", serverUrl);
            return ToolCallbackProvider.from(List.<ToolCallback>of());
        }

        try {
            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(serverUrl)
                    .sseEndpoint(sseEndpoint)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            McpSyncClient client = McpClient.sync(transport)
                    .clientInfo(new McpSchema.Implementation("spring-ai-demo-client", "1.0.0"))
                    .requestTimeout(Duration.ofSeconds(30))
                    .build();
            // 握手：真正建立 SSE 连接并拿到服务端能力，失败会抛异常走下面的兜底
            client.initialize();
            List<String> toolNames = client.listTools().tools().stream()
                    .map(McpSchema.Tool::name)
                    .toList();
            this.mcpSyncClient = client;
            log.info("已连接 MCP 工具服务 {}，注册工具 {}", serverUrl, toolNames);
            return new SyncMcpToolCallbackProvider(client);
        }
        catch (Exception e) {
            log.warn("连接 MCP 工具服务 {} 失败，本次启动不注册工具，AI 对话仍可正常使用：{}",
                    serverUrl, e.getMessage());
            return ToolCallbackProvider.from(List.<ToolCallback>of());
        }
    }

    @PreDestroy
    public void close() {
        if (mcpSyncClient != null) {
            try {
                mcpSyncClient.closeGracefully();
            }
            catch (Exception e) {
                log.debug("关闭 MCP 客户端异常", e);
            }
        }
    }

    /**
     * TCP 连通性检查：2 秒超时，只检测端口是否开放，不发送任何数据。
     */
    private boolean isReachable(String url) {
        try {
            URI uri = new URI(url);
            int port = uri.getPort();
            if (port <= 0) port = 8082;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(uri.getHost(), port), 2000);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}