package com.chartflow.core.model.dto.chart;

import lombok.Data;

/**
 * 图表生成消息 DTO
 */
@Data
public class ChartGenMessage {

    /**
     * 图表ID
     */
    private Long chartId;

    /**
     * 分析模板ID（可选）
     */
    private Long promptId;

    public ChartGenMessage() {
    }

    public ChartGenMessage(Long chartId, Long promptId) {
        this.chartId = chartId;
        this.promptId = promptId;
    }
}
