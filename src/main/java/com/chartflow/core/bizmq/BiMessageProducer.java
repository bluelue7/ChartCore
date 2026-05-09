package com.chartflow.core.bizmq;

import com.chartflow.core.model.dto.chart.ChartGenMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BiMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 发送消息（仅chartId）
     * @param message
     */
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, message);
    }

    /**
     * 发送消息（包含chartId和promptId）
     * @param chartId
     * @param promptId
     */
    public void sendMessage(Long chartId, Long promptId) {
        try {
            ChartGenMessage chartGenMessage = new ChartGenMessage(chartId, promptId);
            String message = objectMapper.writeValueAsString(chartGenMessage);
            rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, message);
        } catch (Exception e) {
            // 如果序列化失败，降级为只发送chartId
            rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, chartId.toString());
        }
    }

}
