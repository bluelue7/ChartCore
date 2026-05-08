package com.chartflow.core.model.dto.tasklog;

import com.chartflow.core.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 任务执行日志查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskLogQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 对应chart.id
     */
    private String chartId;

    /**
     * running/success/failed
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
