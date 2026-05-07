package com.chartflow.core.model.dto.dataset;

import com.chartflow.core.common.PageRequest;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据集查询请求
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DatasetQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 数据集名称（模糊搜索）
     */
    private String name;

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
