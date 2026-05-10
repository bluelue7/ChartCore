package com.chartflow.core.service.impl;

import com.chartflow.core.mcp.McpClient;
import com.chartflow.core.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Resource
    private McpClient mcpClient;

    @Override
    public boolean sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        log.info("尝试发送邮件: to={}, subject={}", toEmail, subject);
        if (mcpClient == null) {
            log.warn("MCP 客户端未初始化");
            return false;
        }
        boolean result = mcpClient.sendEmail(toEmail, subject, htmlContent);
        log.info("邮件发送结果: {}", result ? "成功" : "失败");
        return result;
    }

    @Override
    public boolean isAvailable() {
        return mcpClient != null && mcpClient.isInitialized();
    }
}