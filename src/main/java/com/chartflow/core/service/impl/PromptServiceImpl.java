package com.chartflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.constant.CommonConstant;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.mapper.PromptMapper;
import com.chartflow.core.model.dto.prompt.PromptQueryRequest;
import com.chartflow.core.model.entity.Prompt;
import com.chartflow.core.service.PromptService;
import com.chartflow.core.utils.SqlUtils;
import java.util.Date;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @author bluelue7
* @description 针对表【prompt(分析模板表)】的数据库操作Service实现
* @createDate 2026-05-07
*/
@Service
@Slf4j
public class PromptServiceImpl extends ServiceImpl<PromptMapper, Prompt> implements PromptService {

    @Override
    public QueryWrapper<Prompt> getQueryWrapper(PromptQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = queryRequest.getId();
        String name = queryRequest.getName();
        Long userId = queryRequest.getUserId();
        Date createTimeStart = queryRequest.getCreateTimeStart();
        Date createTimeEnd = queryRequest.getCreateTimeEnd();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        QueryWrapper<Prompt> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.ge(createTimeStart != null, "createTime", createTimeStart);
        queryWrapper.le(createTimeEnd != null, "createTime", createTimeEnd);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public void incrementUsageCount(Long promptId) {
        if (promptId == null || promptId <= 0) {
            return;
        }
        Prompt prompt = this.getById(promptId);
        if (prompt != null) {
            int newCount = (prompt.getUsageCount() == null ? 0 : prompt.getUsageCount()) + 1;
            prompt.setUsageCount(newCount);
            this.updateById(prompt);
        }
    }
}
