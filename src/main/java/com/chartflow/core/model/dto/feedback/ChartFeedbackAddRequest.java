package com.chartflow.core.model.dto.feedback;

import java.io.Serializable;
import lombok.Data;

/**
 * 图表反馈添加请求
 *
 */
@Data
public class ChartFeedbackAddRequest implements Serializable {

    /**
     * 图表ID
     */
    private Long chartId;

    /**
     * 评分：1-5分
     */
    private Integer rating;

    /**
     * 反馈意见
     */
    private String comment;

    private static final long serialVersionUID = 1L;
}
