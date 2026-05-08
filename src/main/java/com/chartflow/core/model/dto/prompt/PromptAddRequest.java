package com.chartflow.core.model.dto.prompt;

import java.io.Serializable;
import lombok.Data;

/**
 * 模板添加请求
 *
 */
@Data
public class PromptAddRequest implements Serializable {

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板自然语言查询（可包含占位符）
     */
    private String promptQuery;

    private static final long serialVersionUID = 1L;
}
