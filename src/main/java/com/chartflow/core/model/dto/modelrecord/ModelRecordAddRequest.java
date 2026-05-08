package com.chartflow.core.model.dto.modelrecord;

import lombok.Data;

import java.io.Serializable;

/**
 * 大模型调用记录添加请求
 */
@Data
public class ModelRecordAddRequest implements Serializable {

    /**
     * 关联的图表分析任务ID（chart.id）
     */
    private Long chartId;

    /**
     * 发起调用的用户ID
     */
    private Long userId;

    /**
     * 使用的模型名称，如 qwen2:7b
     */
    private String modelName;

    /**
     * 调用类型：intent/codeGen/insight
     */
    private String invocationType;

    /**
     * 输入Token数量
     */
    private Integer inputTokens;

    /**
     * 输出Token数量
     */
    private Integer outputTokens;

    /**
     * 总计Token数量
     */
    private Integer totalTokens;

    /**
     * 调用耗时（毫秒）
     */
    private Integer costMs;

    /**
     * 调用状态：running/success/failed
     */
    private String status;

    /**
     * 请求内容（Prompt等）
     */
    private String requestContent;

    /**
     * 响应内容
     */
    private String responseContent;

    /**
     * 错误信息
     */
    private String errorMessage;

    private static final long serialVersionUID = 1L;
}
