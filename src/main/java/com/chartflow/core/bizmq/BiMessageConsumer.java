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
import com.chartflow.core.model.vo.AiResponse;
import com.chartflow.core.service.ChartService;
import com.chartflow.core.service.ModelRecordService;
import com.chartflow.core.service.PromptService;
import com.chartflow.core.service.TaskLogService;
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
            handleChartUpdateError(chart.getId(), "更新图表执行中状态失败", taskLog, modelRecord, startTime);
            return;
        }
        try {
            AiResponse aiResponse = aiManager.doChartChatWithInfo(chart.getGoal(), chart.getChartData(), promptQuery);
            String result = aiResponse.getContent();
            //log.info("receiveMessage result = {}", result);
            String[] splits = result.split("【【【【【");
            if (splits.length < 3) {
                channel.basicNack(deliveryTag, false, false);
                handleChartUpdateError(chart.getId(), "AI 生成错误", taskLog, modelRecord, startTime);
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                channel.basicNack(deliveryTag, false, false);
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败", taskLog, modelRecord, startTime);
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

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), e.getMessage(), taskLog, modelRecord, startTime);
            throw e;
        }
    }


    private void handleChartUpdateError(long chartId, String execMessage, TaskLog taskLog, ModelRecord modelRecord, long startTime) {
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
            modelRecordService.updateModelRecordStatus(modelRecord.getId(), "failed", null, execMessage, costMs);
        }
    }
}
