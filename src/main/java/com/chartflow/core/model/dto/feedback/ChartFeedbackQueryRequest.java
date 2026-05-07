package com.chartflow.core.model.dto.feedback;

import com.chartflow.core.common.PageRequest;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图表反馈查询请求
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartFeedbackQueryRequest extends PageRequest implements Serializable {

    /**
     * 反馈ID
     */
    private Long id;

    /**
     * 图表ID
     */
    private Long chartId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 评分
     */
    private Integer rating;

    /**
     * 创建时间开始
     */
    private Date createTimeStart;

    /**
     * 创建时间结束
     */
    private Date createTimeEnd;

    private static final long serialVersionUID = 1L;
}
