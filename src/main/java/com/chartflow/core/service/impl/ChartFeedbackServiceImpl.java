package com.chartflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.constant.CommonConstant;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.mapper.ChartFeedbackMapper;
import com.chartflow.core.model.dto.feedback.ChartFeedbackQueryRequest;
import com.chartflow.core.model.entity.ChartFeedback;
import com.chartflow.core.service.ChartFeedbackService;
import com.chartflow.core.utils.SqlUtils;
import java.util.Date;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @author bluelue7
* @description 针对表【chart_feedback(图表反馈表)】的数据库操作Service实现
* @createDate 2026-05-07
*/
@Service
@Slf4j
public class ChartFeedbackServiceImpl extends ServiceImpl<ChartFeedbackMapper, ChartFeedback> implements ChartFeedbackService {

    @Override
    public QueryWrapper<ChartFeedback> getQueryWrapper(ChartFeedbackQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = queryRequest.getId();
        Long chartId = queryRequest.getChartId();
        Long userId = queryRequest.getUserId();
        Integer rating = queryRequest.getRating();
        Date createTimeStart = queryRequest.getCreateTimeStart();
        Date createTimeEnd = queryRequest.getCreateTimeEnd();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        QueryWrapper<ChartFeedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(chartId != null, "chartId", chartId);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(rating != null, "rating", rating);
        queryWrapper.ge(createTimeStart != null, "createTime", createTimeStart);
        queryWrapper.le(createTimeEnd != null, "createTime", createTimeEnd);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
