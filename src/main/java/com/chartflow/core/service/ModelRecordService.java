package com.chartflow.core.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chartflow.core.model.dto.modelrecord.ModelRecordAddRequest;
import com.chartflow.core.model.dto.modelrecord.ModelRecordQueryRequest;
import com.chartflow.core.model.entity.ModelRecord;

/**
 * 大模型调用记录服务
 */
public interface ModelRecordService extends IService<ModelRecord> {

    /**
     * 获取查询条件封装
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<ModelRecord> getQueryWrapper(ModelRecordQueryRequest queryRequest);

    /**
     * 添加模型调用记录
     *
     * @param addRequest 添加请求
     * @return 记录ID
     */
    Long addModelRecord(ModelRecordAddRequest addRequest);

    /**
     * 更新模型调用记录状态
     *
     * @param id 记录ID
     * @param status 状态
     * @param responseContent 响应内容
     * @param errorMessage 错误信息
     * @param costMs 耗时
     * @return 是否成功
     */
    boolean updateModelRecordStatus(Long id, String status, String responseContent, String errorMessage, Integer costMs);

    /**
     * 更新模型调用记录状态（带Token信息）
     *
     * @param id 记录ID
     * @param status 状态
     * @param responseContent 响应内容
     * @param errorMessage 错误信息
     * @param costMs 耗时
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     * @param totalTokens 总Token数
     * @return 是否成功
     */
    boolean updateModelRecordStatus(Long id, String status, String responseContent, String errorMessage, 
                                    Integer costMs, Integer inputTokens, Integer outputTokens, Integer totalTokens);
}
