package com.chartflow.core.model.dto.feedback;

import java.io.Serializable;
import lombok.Data;

/**
 * 图表反馈更新请求
 *
 */
@Data
public class ChartFeedbackUpdateRequest implements Serializable {

    /**
     * 反馈ID
     */
    private Long id;

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
