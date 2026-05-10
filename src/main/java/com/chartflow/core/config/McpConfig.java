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

    @Value("${mcp.email.smtp-server:smtp.qq.com}")
    private String smtpServer;

    @Value("${mcp.email.smtp-port:587}")
    private String smtpPort;

    @Value("${mcp.email.smtp-user:}")
    private String smtpUser;

    @Value("${mcp.email.smtp-password:}")
    private String smtpPassword;


    private McpClient mcpClient;

    @Bean
    public McpClient mcpClient() {
        try {
             // 检查配置是否完整
            if (smtpUser.isEmpty() || smtpPassword.isEmpty()) {
                log.warn("SMTP 配置未完整设置，邮件功能将不可用");
                return null;
            }
            
            mcpClient = new McpClient(serverPath, smtpServer, smtpPort, smtpUser, smtpPassword);
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