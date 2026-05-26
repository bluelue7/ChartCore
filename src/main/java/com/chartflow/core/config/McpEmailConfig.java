package com.chartflow.core.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 邮件服务配置类
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.email")
@Data
public class McpEmailConfig {

    /**
     * MCP 服务脚本路径
     */
    private String serverPath = "./mcp_server/mcp_email_server.py";

    /**
     * SMTP 服务器地址
     */
    private String smtpServer = "smtp.qq.com";

    /**
     * SMTP 端口
     */
    private String smtpPort = "587";

    /**
     * SMTP 用户名
     */
    private String smtpUser;

    /**
     * SMTP 密码（授权码）
     */
    private String smtpPassword;

    /**
     * 检查配置是否完整
     */
    public boolean isConfigured() {
        return StringUtils.isNotBlank(smtpServer) 
                && StringUtils.isNotBlank(smtpPort) 
                && StringUtils.isNotBlank(smtpUser) 
                && StringUtils.isNotBlank(smtpPassword);
    }
}