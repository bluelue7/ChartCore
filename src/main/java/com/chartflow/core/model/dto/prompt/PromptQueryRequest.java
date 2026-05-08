package com.chartflow.core.model.dto.prompt;

import com.chartflow.core.common.PageRequest;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板查询请求
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PromptQueryRequest extends PageRequest implements Serializable {

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板名称（模糊搜索）
     */
    private String name;

    /**
     * 创建用户ID
     */
    private Long userId;

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
