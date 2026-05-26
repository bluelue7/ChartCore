package com.chartflow.core.service.impl;

import com.chartflow.core.config.ChartConfig;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.model.dto.chart.OllamaChatRequest;
import com.chartflow.core.model.vo.AiResponse;
import com.chartflow.core.model.vo.OllamaChatResponse;
import com.chartflow.core.service.LocalAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/**
 * 本地 AI 服务实现
 * 对接 Ollama 本地大模型服务
 */
@Slf4j
@Service
public class LocalAiServiceImpl implements LocalAiService {

    @Value("${ollama.base-url:http://localhost:11434/v1}")
    private String baseUrl;

    @Value("${ollama.model:qwen2.5:7b}")
    private String model;

    private final RestTemplate restTemplate;
    private final ChartConfig chartConfig;

    public LocalAiServiceImpl(RestTemplate restTemplate, ChartConfig chartConfig) {
        this.restTemplate = restTemplate;
        this.chartConfig = chartConfig;
    }

    @Override
    public String doChat(String message) {
        return doChatWithInfo(message).getContent();
    }

    @Override
    public String doChartChat(String goal, String csvData) {
        return doChartChatWithInfo(goal, csvData).getContent();
    }

    @Override
    public boolean testConnection() {
        try {
            String url = baseUrl + "/models";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("测试本地AI连接失败", e);
            return false;
        }
    }

    @Override
    public AiResponse doChatWithInfo(String message) {
        try {
            String url = baseUrl + "/chat/completions";

            // 构建符合OpenAI格式的请求
            OllamaChatRequest request = new OllamaChatRequest();
            request.setModel(model);
            request.setMessages(Arrays.asList(
                    new OllamaChatRequest.Message("user", message)
            ));
            request.setStream(false);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<OllamaChatRequest> entity = new HttpEntity<>(request, headers);

            log.info("调用本地AI，模型: {}, 消息长度: {}", model, message.length());

            // 发送请求
            ResponseEntity<OllamaChatResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, OllamaChatResponse.class);

            if (response.getBody() != null &&
                    response.getBody().getChoices() != null &&
                    !response.getBody().getChoices().isEmpty()) {

                String content = response.getBody().getChoices().get(0).getMessage().getContent();
                log.info("本地AI返回成功，内容长度: {}", content.length());

                // 提取Token信息
                OllamaChatResponse.Usage usage = response.getBody().getUsage();
                Integer inputTokens = usage != null ? usage.getPrompt_tokens() : null;
                Integer outputTokens = usage != null ? usage.getCompletion_tokens() : null;
                Integer totalTokens = usage != null ? usage.getTotal_tokens() : null;

                return AiResponse.success(content, inputTokens, outputTokens, totalTokens);
            }

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI返回内容为空");

        } catch (Exception e) {
            log.error("调用本地AI失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI服务调用失败");
        }
    }

    @Override
    public AiResponse doChartChatWithInfo(String goal, String csvData) {
        // 使用配置类中的默认 prompt
        String prompt = buildChartPrompt(goal, csvData);
        return doChatWithInfo(prompt);
    }

    /**
     * 构建图表生成的提示词
     */
    private String buildChartPrompt(String goal, String csvData) {
        // 使用配置类中的默认模板
        String defaultPrompt = chartConfig.getDefaultPrompt();
        
        // 如果配置中的模板已经包含占位符，替换占位符
        return defaultPrompt
                .replace("{数据分析的需求或者目标}", goal)
                .replace("{csv格式的原始数据，用,作为分隔符}", csvData);
    }

    @Override
    public String doChartChat(String goal, String csvData, String customPrompt) {
        return doChartChatWithInfo(goal, csvData, customPrompt).getContent();
    }

    @Override
    public AiResponse doChartChatWithInfo(String goal, String csvData, String customPrompt) {
        String prompt;
        if (customPrompt != null && !customPrompt.isEmpty()) {
            // 使用自定义prompt，拼接分析目标和数据
            prompt = customPrompt + "\n====================\n" +
                    "分析需求：\n" + goal + "\n" +
                    "原始数据：\n" + csvData;
        } else {
            // 使用配置类中的默认模板
            prompt = buildChartPrompt(goal, csvData);
        }
        log.debug("构建的图表生成提示词长度: {}", prompt.length());
        return doChatWithInfo(prompt);
    }
}