package com.chartflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chartflow.core.mapper.ModelRecordMapper;
import com.chartflow.core.model.dto.modelrecord.ModelRecordAddRequest;
import com.chartflow.core.model.dto.modelrecord.ModelRecordQueryRequest;
import com.chartflow.core.model.entity.ModelRecord;
import com.chartflow.core.service.ModelRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 大模型调用记录服务实现
 */
@Service
@Slf4j
public class ModelRecordServiceImpl extends ServiceImpl<ModelRecordMapper, ModelRecord> implements ModelRecordService {

    @Override
    public QueryWrapper<ModelRecord> getQueryWrapper(ModelRecordQueryRequest queryRequest) {
        QueryWrapper<ModelRecord> queryWrapper = new QueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        Long id = queryRequest.getId();
        Long chartId = queryRequest.getChartId();
        Long userId = queryRequest.getUserId();
        String modelName = queryRequest.getModelName();
        String invocationType = queryRequest.getInvocationType();
        String status = queryRequest.getStatus();
        Date createTimeStart = queryRequest.getCreateTimeStart();
        Date createTimeEnd = queryRequest.getCreateTimeEnd();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(chartId != null && chartId > 0, "chartId", chartId);
        queryWrapper.eq(userId != null && userId > 0, "userId", userId);
        queryWrapper.eq(StringUtils.isNotBlank(modelName), "modelName", modelName);
        queryWrapper.eq(StringUtils.isNotBlank(invocationType), "invocationType", invocationType);
        queryWrapper.eq(StringUtils.isNotBlank(status), "status", status);
        queryWrapper.ge(createTimeStart != null, "createTime", createTimeStart);
        queryWrapper.le(createTimeEnd != null, "createTime", createTimeEnd);
        queryWrapper.orderByDesc("createTime");
        return queryWrapper;
    }

    @Override
    public Long addModelRecord(ModelRecordAddRequest addRequest) {
        ModelRecord modelRecord = new ModelRecord();
        BeanUtils.copyProperties(addRequest, modelRecord);
        modelRecord.setCreateTime(new Date());
        modelRecord.setUpdateTime(new Date());
        boolean save = this.save(modelRecord);
        if (!save) {
            log.error("添加模型调用记录失败");
            return null;
        }
        return modelRecord.getId();
    }

    @Override
    public boolean updateModelRecordStatus(Long id, String status, String responseContent, String errorMsg, Integer costMs) {
        return updateModelRecordStatus(id, status, responseContent, errorMsg, costMs, null, null, null);
    }

    @Override
    public boolean updateModelRecordStatus(Long id, String status, String responseContent, String errorMsg, 
                                           Integer costMs, Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        ModelRecord modelRecord = new ModelRecord();
        modelRecord.setId(id);
        modelRecord.setStatus(status);
        modelRecord.setResponseContent(responseContent);
        modelRecord.setErrorMsg(errorMsg);
        modelRecord.setCostMs(costMs);
        modelRecord.setInputTokens(inputTokens);
        modelRecord.setOutputTokens(outputTokens);
        modelRecord.setTotalTokens(totalTokens);
        modelRecord.setUpdateTime(new Date());
        return this.updateById(modelRecord);
    }
}
