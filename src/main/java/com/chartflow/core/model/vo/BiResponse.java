package com.chartflow.core.model.vo;

import lombok.Data;

/**
 * Bi 的返回结果
 */
@Data
public class BiResponse {

    /*
     * 生成图表
     */
    private String genChart;

    /*
     * 生成结果
     */
    private String genResult;

    /*
     * 图表id
     */
    private Long chartId;
}

