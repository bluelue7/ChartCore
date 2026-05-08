package com.chartflow.core.model.dto.tasklog;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务执行日志添加请求
 */
@Data
public class TaskLogAddRequest implements Serializable {

    /**
     * 对应chart.id
     */
    private String chartId;

    /**
     * running/success/failed
     */
    private String status;

    /**
     * 耗时（毫秒）
     */
    private Integer costMs;

    /**
     * 执行信息
     */
    private String execMessage;

    private static final long serialVersionUID = 1L;
}
