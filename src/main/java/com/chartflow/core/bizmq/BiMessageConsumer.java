package com.chartflow.core.bizmq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chartflow.core.model.dto.chart.ChartGenMessage;
import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.model.entity.ModelRecord;
import com.chartflow.core.model.entity.TaskLog;
import com.chartflow.core.model.entity.User;
import com.chartflow.core.model.vo.ChartGenResult;
import com.chartflow.core.service.ChartGenService;
import com.chartflow.core.service.ChartService;
import com.chartflow.core.service.EmailService;
import com.chartflow.core.service.ModelRecordService;
import com.chartflow.core.service.TaskLogService;
import com.chartflow.core.service.UserService;
import com.chartflow.core.utils.EmailContentBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * BI 消息消费者
 * 负责接收MQ消息，调用服务层处理图表生成任务
 * 使用线程池异步处理，提高并发处理能力
 */
@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private ChartGenService chartGenService;

    @Resource
    private TaskLogService taskLogService;

    @Resource
    private ModelRecordService modelRecordService;

    @Resource
    private UserService userService;

    @Resource
    private EmailService emailService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 用于处理图表生成任务的线程池
     */
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 消息接收入口
     * 负责解析消息和初步校验，然后将任务提交到线程池处理
     */
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, 
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到BI消息: {}", message);
        
        // 1. 基本校验
        if (StringUtils.isBlank(message)) {
            log.error("消息为空，拒绝消息");
            safeNack(channel, deliveryTag);
            return;
        }

        // 2. 解析消息
        Long chartId = null;
        Long promptId = null;
        try {
            ChartGenMessage chartGenMessage = objectMapper.readValue(message, ChartGenMessage.class);
            chartId = chartGenMessage.getChartId();
            promptId = chartGenMessage.getPromptId();
        } catch (Exception e) {
            try {
                chartId = Long.parseLong(message);
            } catch (NumberFormatException ex) {
                log.error("消息格式错误: {}", message);
                safeNack(channel, deliveryTag);
                return;
            }
        }

        // 3. 查询图表信息
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            log.error("图表不存在: chartId={}", chartId);
            safeNack(channel, deliveryTag);
            return;
        }

        // 4. 查询关联记录
        TaskLog taskLog = taskLogService.getOne(new LambdaQueryWrapper<TaskLog>()
                .eq(TaskLog::getChartId, String.valueOf(chartId)));
        ModelRecord modelRecord = modelRecordService.getOne(new LambdaQueryWrapper<ModelRecord>()
                .eq(ModelRecord::getChartId, chartId));

        // 5. 更新图表状态为运行中
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("running");
        chartService.updateById(updateChart);

        // 6. 将任务提交到线程池处理
        final Long finalChartId = chartId;
        final Long finalPromptId = promptId;
        final TaskLog finalTaskLog = taskLog;
        final ModelRecord finalModelRecord = modelRecord;

        threadPoolExecutor.submit(() -> processChartGenTask(finalChartId, finalPromptId, 
                finalTaskLog, finalModelRecord, channel, deliveryTag));
        
        log.info("图表生成任务已提交到线程池: chartId={}", chartId);
    }

    /**
     * 处理图表生成任务
     * 调用服务层执行带重试的图表生成
     */
    private void processChartGenTask(Long chartId, Long promptId, TaskLog taskLog, 
                                     ModelRecord modelRecord, Channel channel, long deliveryTag) {
        long startTime = System.currentTimeMillis();
        log.info("开始处理图表生成任务: chartId={}", chartId);

        try {
            // 调用服务层处理（包含重试、容错逻辑）
            // 注意：此调用会执行AI生成，耗时较长，不需要保持数据库连接
            ChartGenResult result = chartGenService.processChartGenTask(chartId, promptId);
            
            // AI生成完成后，再批量更新数据库
            // 更新图表结果
            Chart updateChart = new Chart();
            updateChart.setId(chartId);
            updateChart.setGenChart(result.getGenChart());
            updateChart.setGenResult(result.getGenResult());
            updateChart.setStatus(result.getStatus());
            chartService.updateById(updateChart);
            
            // 更新任务记录
            int costMs = (int) (System.currentTimeMillis() - startTime);
            updateTaskRecords(taskLog, modelRecord, result, costMs);
            
            // 先确认消息，避免邮件失败导致重复消费
            safeAck(channel, deliveryTag);
            
            // 发送邮件通知（异步，不阻塞）
            sendEmailNotificationAsync(chartId);
            
            log.info("图表生成任务处理完成: chartId={}, status={}, 耗时={}ms", chartId, result.getStatus(), costMs);

        } catch (Exception e) {
            log.error("处理图表生成任务异常: chartId={}", chartId, e);
            handleError(chartId, taskLog, modelRecord, startTime, e.getMessage(), channel, deliveryTag);
        }
    }

    /**
     * 更新任务记录
     */
    private void updateTaskRecords(TaskLog taskLog, ModelRecord modelRecord, 
                                  ChartGenResult result, int costMs) {
        if (taskLog != null) {
            taskLogService.updateTaskLogStatus(taskLog.getId(), result.getStatus(), costMs, 
                    result.getErrorMessage());
        }
        if (modelRecord != null) {
            modelRecordService.updateModelRecordStatus(
                    modelRecord.getId(), 
                    result.getStatus(), 
                    result.getAiRawResponse(), 
                    result.getErrorMessage(), 
                    costMs,
                    result.getInputTokens(),
                    result.getOutputTokens(),
                    result.getTotalTokens()
            );
        }
    }

    /**
     * 处理错误
     */
    private void handleError(Long chartId, TaskLog taskLog, ModelRecord modelRecord, 
                            long startTime, String errorMessage, Channel channel, long deliveryTag) {
        // 更新图表状态
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setStatus("failed");
        chart.setExecMessage(errorMessage);
        chartService.updateById(chart);
        
        // 更新任务记录
        int costMs = (int) (System.currentTimeMillis() - startTime);
        if (taskLog != null) {
            taskLogService.updateTaskLogStatus(taskLog.getId(), "failed", costMs, errorMessage);
        }
        if (modelRecord != null) {
            modelRecordService.updateModelRecordStatus(modelRecord.getId(), "failed", null, errorMessage, costMs);
        }
        
        // 拒绝消息
        safeNack(channel, deliveryTag);
        log.error("图表生成任务失败: chartId={}, error={}", chartId, errorMessage);
    }

    /**
     * 安全确认消息
     */
    private void safeAck(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("确认消息失败", e);
        }
    }

    /**
     * 安全拒绝消息
     */
    private void safeNack(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.error("拒绝消息失败", e);
        }
    }

    /**
     * 异步发送邮件通知
     */
    private void sendEmailNotificationAsync(Long chartId) {
        threadPoolExecutor.submit(() -> {
            try {
                Chart chart = chartService.getById(chartId);
                if (chart == null) {
                    log.warn("发送邮件通知失败: 图表不存在, chartId={}", chartId);
                    return;
                }

                User user = userService.getById(chart.getUserId());
                if (user == null || StringUtils.isBlank(user.getEmail())) {
                    log.warn("发送邮件通知失败: 用户未绑定邮箱, chartId={}, userId={}", chartId, chart.getUserId());
                    return;
                }

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
        });
    }
}