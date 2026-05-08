package com.chartflow.core.service;

import com.chartflow.core.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chartflow.core.model.dto.chart.ChartQueryRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
* @author bluelue7
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2025-12-01 15:14:26
*/
public interface ChartService extends IService<Chart> {

    /**
     * 获取查询条件封装
     *
     * @param chartQueryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);
}
