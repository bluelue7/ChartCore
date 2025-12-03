package com.chartflow.core.service.impl;

import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.model.dto.chart.OllamaChatRequest;
import com.chartflow.core.model.vo.OllamaChatResponse;
import com.chartflow.core.service.LocalAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Slf4j
@Service
public class LocalAiServiceImpl implements LocalAiService {
    @Value("${ollama.base-url:http://localhost:11434/v1}")
    private String baseUrl;

    @Value("${ollama.model:qwen2.5:7b}")
    private String model;

    private final RestTemplate restTemplate;

    public LocalAiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String doChat(String message) {
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
                return content;
            }

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI返回内容为空");

        } catch (Exception e) {
            log.error("调用本地AI失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI服务调用失败");
        }
    }

    @Override
    public String doChartChat(String goal, String csvData) {
        // 构建专门的图表生成提示词
        String prompt = buildChartPrompt(goal, csvData);
        return doChat(prompt);
    }

    /**
     * 构建图表生成的提示词
     */
    private String buildChartPrompt(String goal, String csvData) {
        return "你是一个专业的数据分析师和前端开发专家。请根据以下分析需求和原始数据，生成图表配置和分析结论。\n\n" +
                "分析需求：\n" + goal + "\n\n" +
                "原始数据：\n" + csvData + "\n\n" +
                "请严格按照以下格式生成内容（不要输出任何其他文字）：\n" +
                "【【【【【\n" +
                "{这里输出ECharts V5的option配置对象JSON代码，合理地进行数据可视化}\n" +
                "【【【【【\n" +
                "{这里输出详细的数据分析结论}";
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
}
