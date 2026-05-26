package com.chartflow.core.mcp;

import com.chartflow.core.config.McpEmailConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP 客户端
 * 用于与 Python MCP 服务通信，发送邮件通知
 */
@Slf4j
public class McpClient {

    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;
    private ObjectMapper objectMapper;
    private boolean initialized = false;

    /**
     * 使用配置类初始化 MCP 客户端
     */
    public McpClient(McpEmailConfig config) throws IOException {
        this.objectMapper = new ObjectMapper();
        
        // 检查配置是否完整
        if (!config.isConfigured()) {
            log.warn("MCP 邮件配置不完整，跳过初始化");
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("python", config.getServerPath());
            
            // 设置 SMTP 环境变量
            Map<String, String> env = pb.environment();
            env.put("SMTP_SERVER", config.getSmtpServer());
            env.put("SMTP_PORT", config.getSmtpPort());
            env.put("SMTP_USER", config.getSmtpUser());
            env.put("SMTP_PASSWORD", config.getSmtpPassword());
            
            pb.redirectErrorStream(false);
            this.process = pb.start();
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "UTF-8"));
            
            // 读取并忽略启动时的非 JSON 输出
            initialize();
        } catch (IOException e) {
            log.warn("MCP 客户端初始化失败: {}", e.getMessage());
            throw e;
        }
    }

    private void initialize() throws IOException {
        // 清空启动时可能输出的日志（非 JSON 内容）
        reader.mark(8192);
        long startTime = System.currentTimeMillis();
        
        // 读取所有可用的非 JSON 输出
        while (System.currentTimeMillis() - startTime < 1000) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line == null) break;
                
                // 检查是否是 JSON（以 { 开头）
                if (line.trim().startsWith("{")) {
                    // 是 JSON，重置读取位置
                    reader.reset();
                    reader.readLine(); // 重新读取这行
                    break;
                }
                
                // 不是 JSON，记录日志并继续
                log.debug("MCP 启动日志: {}", line);
                reader.mark(8192);
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 发送初始化请求
        Map<String, Object> request = new HashMap<>();
        request.put("type", "initialize");
        JsonNode response = sendRequest(request);

        if ("initialize_response".equals(response.get("type").asText())) {
            initialized = true;
            log.info("MCP 服务器初始化成功");
        }
    }

    private JsonNode sendRequest(Map<String, Object> request) throws IOException {
        String jsonRequest = objectMapper.writeValueAsString(request);
        log.debug("发送 MCP 请求: {}", jsonRequest);

        writer.write(jsonRequest);
        writer.newLine();
        writer.flush();
        
        String responseLine = reader.readLine();
        if (responseLine == null) {
            throw new IOException("MCP 服务器连接中断");
        }

        log.debug("收到 MCP 响应: {}", responseLine);
        return objectMapper.readTree(responseLine);
    }

    /**
     * 发送邮件
     *
     * @param toEmail     收件人邮箱
     * @param subject     邮件主题
     * @param htmlContent HTML 内容
     * @return 是否发送成功
     */
    public boolean sendEmail(String toEmail, String subject, String htmlContent) {
        if (!initialized) {
            log.warn("MCP 客户端未初始化");
            return false;
        }
        
        // 参数校验
        if (StringUtils.isBlank(toEmail) || StringUtils.isBlank(subject)) {
            log.warn("邮件参数不完整: toEmail={}, subject={}", toEmail, subject);
            return false;
        }
        
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("type", "tool_call");
            request.put("name", "send_email");
            
            Map<String, String> arguments = new HashMap<>();
            arguments.put("to_email", toEmail);
            arguments.put("subject", subject);
            arguments.put("html_content", htmlContent);
            request.put("arguments", arguments);
            
            JsonNode response = sendRequest(request);
            String type = response.get("type").asText();
            if ("tool_result".equals(type)) {
                boolean success = response.get("success").asBoolean();
                if (!success && response.has("content")) {
                    log.error("邮件发送失败: {}", response.get("content").asText());
                }
                return success;
            }
            return false;
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            return false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void close() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (process != null) process.destroy();
        } catch (IOException e) {
            log.error("关闭 MCP 客户端失败", e);
        }
    }
}