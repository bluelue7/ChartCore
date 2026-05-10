package com.chartflow.core.config;

import com.chartflow.core.mcp.McpClient;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class McpConfig {

    @Value("${mcp.email.server-path:./mcp_server/mcp_email_server.py}")
    private String serverPath;

    private McpClient mcpClient;

    @Bean
    public McpClient mcpClient() {
        try {
            mcpClient = new McpClient(serverPath);
            return mcpClient;
        } catch (IOException e) {
            log.warn("MCP 客户端初始化失败，邮件通知功能将不可用: {}", e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (mcpClient != null) {
            mcpClient.close();
        }
    }
}