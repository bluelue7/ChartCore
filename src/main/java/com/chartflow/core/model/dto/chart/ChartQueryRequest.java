package com.chartflow.core.model.dto.chart;

import com.chartflow.core.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    private Long id;

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 状态
     */
    private String status;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}