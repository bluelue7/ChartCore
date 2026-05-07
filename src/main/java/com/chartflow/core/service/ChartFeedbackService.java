package com.chartflow.core.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chartflow.core.model.dto.feedback.ChartFeedbackQueryRequest;
import com.chartflow.core.model.entity.ChartFeedback;

/**
* @author bluelue7
* @description 针对表【chart_feedback(图表反馈表)】的数据库操作Service
* @createDate 2026-05-07
*/
public interface ChartFeedbackService extends IService<ChartFeedback> {

    /**
     * 获取查询条件包装器
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<ChartFeedback> getQueryWrapper(ChartFeedbackQueryRequest queryRequest);
}
