package com.chartflow.core.bizmq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.manager.AiManager;
import com.chartflow.core.model.dto.chart.ChartGenMessage;
import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.model.entity.ModelRecord;
import com.chartflow.core.model.entity.Prompt;
import com.chartflow.core.model.entity.TaskLog;
import com.chartflow.core.model.entity.User;
import com.chartflow.core.model.vo.AiResponse;
import com.chartflow.core.service.ChartService;
import com.chartflow.core.service.EmailService;
import com.chartflow.core.service.ModelRecordService;
import com.chartflow.core.service.PromptService;
import com.chartflow.core.service.TaskLogService;
import com.chartflow.core.service.UserService;
import com.chartflow.core.utils.EmailContentBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Resource
    private TaskLogService taskLogService;

    @Resource
    private ModelRecordService modelRecordService;

    @Resource
    private PromptService promptService;

    @Resource
    private UserService userService;

    @Resource
    private EmailService emailService;

    @Resource
    private ObjectMapper objectMapper;

    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }

        // 解析消息
        Long chartId = null;
        Long promptId = null;
        try {
            // 尝试解析为 JSON 对象
            ChartGenMessage chartGenMessage = objectMapper.readValue(message, ChartGenMessage.class);
            chartId = chartGenMessage.getChartId();
            promptId = chartGenMessage.getPromptId();
        } catch (Exception e) {
            // 如果解析失败，尝试作为纯 chartId 处理
            try {
                chartId = Long.parseLong(message);
            } catch (NumberFormatException ex) {
                channel.basicNack(deliveryTag, false, false);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息格式错误");
            }
        }

        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }

        TaskLog taskLog = taskLogService.getOne(new LambdaQueryWrapper<TaskLog>()
                .eq(TaskLog::getChartId, String.valueOf(chartId)));
        ModelRecord modelRecord = modelRecordService.getOne(new LambdaQueryWrapper<ModelRecord>()
                .eq(ModelRecord::getChartId, chartId));

        // 根据 promptId 获取 promptQuery
        String promptQuery = null;
        if (promptId != null && promptId > 0) {
            Prompt prompt = promptService.getById(promptId);
            if (prompt != null) {
                promptQuery = prompt.getPromptQuery();
                // 增加使用次数
                promptService.incrementUsageCount(promptId);
            }
        }

        long startTime = System.currentTimeMillis();

        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表执行中状态失败", taskLog, modelRecord, startTime, null);
            return;
        }
        try {
            AiResponse aiResponse = aiManager.doChartChatWithInfo(chart.getGoal(), chart.getChartData(), promptQuery);
            String result = aiResponse.getContent();
            
            // 使用健壮的解析方法提取图表配置和分析结论
            String[] parts = parseAiResponse(result);
            
            if (parts == null || parts[0] == null || parts[1] == null) {
                channel.basicNack(deliveryTag, false, false);
                handleChartUpdateError(chart.getId(), "AI 生成格式错误，无法解析", taskLog, modelRecord, startTime, result);
                return;
            }
            
            String genChart = parts[0];
            String genResult = parts[1];
            
            // 验证图表配置是否为有效的 JSON
            if (!isValidJson(genChart)) {
                log.warn("图表配置不是有效的 JSON: {}", genChart.substring(0, Math.min(100, genChart.length())));
            }
            
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                channel.basicNack(deliveryTag, false, false);
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败", taskLog, modelRecord, startTime, result);
                return;
            }

            int costMs = (int)(System.currentTimeMillis() - startTime);
            if (taskLog != null) {
                taskLogService.updateTaskLogStatus(taskLog.getId(), "success", costMs, "执行成功");
            }
            if (modelRecord != null) {
                modelRecordService.updateModelRecordStatus(modelRecord.getId(), "success", result, null, costMs,
                        aiResponse.getInputTokens(), aiResponse.getOutputTokens(), aiResponse.getTotalTokens());
            }

            // 发送邮件通知
            sendEmailNotification(chart.getId());

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), e.getMessage(), taskLog, modelRecord, startTime, null);
            throw e;
        }
    }

    /**
     * 健壮地解析 AI 返回的内容，支持多种格式变化
     */
    private String[] parseAiResponse(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        String delimiter = "【【【【【";
        
        // 方法1：严格按分隔符分割
        String[] splits = content.split(delimiter);
        if (splits.length >= 3) {
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            if (!genChart.isEmpty() && !genResult.isEmpty()) {
                return new String[]{genChart, genResult};
            }
        }
        
        // 方法2：查找最后出现的分隔符
        int lastIndex = content.lastIndexOf(delimiter);
        if (lastIndex > 0) {
            int secondLast = content.lastIndexOf(delimiter, lastIndex - 1);
            if (secondLast >= 0) {
                String genChart = content.substring(secondLast + delimiter.length(), lastIndex).trim();
                String genResult = content.substring(lastIndex + delimiter.length()).trim();
                if (!genChart.isEmpty() && !genResult.isEmpty()) {
                    return new String[]{genChart, genResult};
                }
            }
        }
        
        // 方法3：使用括号匹配分割
        int firstBrace = content.indexOf("{");
        int lastBrace = content.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            String afterChart = content.substring(lastBrace + 1).trim();
            if (!afterChart.isEmpty()) {
                return new String[]{content.substring(firstBrace, lastBrace + 1), afterChart};
            }
        }
        
        // 方法4：有2个分隔符的情况
        int count = countOccurrences(content, delimiter);
        if (count >= 2) {
            int firstIdx = content.indexOf(delimiter);
            int secondIdx = content.indexOf(delimiter, firstIdx + delimiter.length());
            if (secondIdx > 0) {
                String genChart = content.substring(firstIdx + delimiter.length(), secondIdx).trim();
                String genResult = content.substring(secondIdx + delimiter.length()).trim();
                if (!genChart.isEmpty() && !genResult.isEmpty()) {
                    return new String[]{genChart, genResult};
                }
            }
        }
        
        log.error("无法解析 AI 返回内容: {}", content.substring(0, Math.min(300, content.length())));
        return null;
    }

    private int countOccurrences(String str, String sub) {
        int count = 0, idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    private boolean isValidJson(String str) {
        if (str == null || str.isEmpty()) return false;
        str = str.trim();
        return (str.startsWith("{") && str.endsWith("}")) ||
               (str.startsWith("[") && str.endsWith("]"));
    }

    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(Long chartId) {
        try {
            Chart chart = chartService.getById(chartId);
            if (chart == null) {
                log.warn("发送邮件通知失败: 图表不存在, chartId={}", chartId);
                return;
            }

            // 获取用户信息
            User user = userService.getById(chart.getUserId());
            if (user == null || StringUtils.isBlank(user.getEmail())) {
                log.warn("发送邮件通知失败: 用户未绑定邮箱, chartId={}, userId={}", chartId, chart.getUserId());
                return;
            }

            // 检查邮件服务是否可用
            if (!emailService.isAvailable()) {
                log.warn("发送邮件通知失败: 邮件服务不可用, chartId={}", chartId);
                return;
            }

            String subject = "图表生成完成 - " + (StringUtils.isNotBlank(chart.getName()) ? chart.getName() : "未命名图表");
            String htmlContent = EmailContentBuilder.buildChartNotificationEmail(chart);
            emailService.sendHtmlEmail(user.getEmail(), subject, htmlContent);
            log.info("邮件通知发送成功: chartId={}, email={}", chartId, user.getEmail());

        } catch (Exception e) {
            log.error("发送邮件通知异常: chartId={}", chartId, e);
        }
    }


    private void handleChartUpdateError(long chartId, String execMessage, TaskLog taskLog, ModelRecord modelRecord, long startTime, String result) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }

        int costMs = (int)(System.currentTimeMillis() - startTime);
        if (taskLog != null) {
            taskLogService.updateTaskLogStatus(taskLog.getId(), "failed", costMs, execMessage);
        }
        if (modelRecord != null) {
            modelRecordService.updateModelRecordStatus(modelRecord.getId(), "failed", result, execMessage, costMs);
        }
    }
}
