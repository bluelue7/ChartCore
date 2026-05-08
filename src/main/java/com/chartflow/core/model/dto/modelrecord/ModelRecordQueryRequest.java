package com.chartflow.core.model.dto.modelrecord;

import com.chartflow.core.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 大模型调用记录查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ModelRecordQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 关联的图表分析任务ID
     */
    private Long chartId;

    /**
     * 发起调用的用户ID
     */
    private Long userId;

    /**
     * 使用的模型名称
     */
    private String modelName;

    /**
     * 调用类型
     */
    private String invocationType;

    /**
     * 调用状态
     */
    private String status;

    /**
     * 创建时间范围（开始）
     */
    private Date createTimeStart;

    /**
     * 创建时间范围（结束）
     */
    private Date createTimeEnd;

    private static final long serialVersionUID = 1L;
}
