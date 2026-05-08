package com.chartflow.core.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chartflow.core.model.dto.prompt.PromptQueryRequest;
import com.chartflow.core.model.entity.Prompt;

/**
* @author bluelue7
* @description 针对表【prompt(分析模板表)】的数据库操作Service
* @createDate 2026-05-07
*/
public interface PromptService extends IService<Prompt> {

    /**
     * 获取查询条件包装器
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<Prompt> getQueryWrapper(PromptQueryRequest queryRequest);

    /**
     * 增加模板使用次数
     *
     * @param promptId 模板ID
     */
    void incrementUsageCount(Long promptId);
}
