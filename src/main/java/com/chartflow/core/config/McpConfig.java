package com.chartflow.core.config;

import com.chartflow.core.mcp.McpClient;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * MCP 客户端配置类
 */
@Configuration
@Slf4j
public class McpConfig {

    @Resource
    private McpEmailConfig mcpEmailConfig;

    private McpClient mcpClient;

    @Bean
    public McpClient mcpClient() {
        try {
            // 使用配置类创建 MCP 客户端
            mcpClient = new McpClient(mcpEmailConfig);
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