package com.chartflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chartflow.core.constant.CommonConstant;

import java.io.Serializable;
import com.chartflow.core.manager.RedisCacheManager;
import com.chartflow.core.model.dto.chart.ChartQueryRequest;
import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.service.ChartService;
import com.chartflow.core.mapper.ChartMapper;
import com.chartflow.core.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
* @author bluelue7
* @description 针对表【chart(图表信息表)】的数据库操作Service实现（集成Redis缓存）
* @createDate 2025-12-01 15:14:26
*/
@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

    @Resource
    private RedisCacheManager redisCacheManager;

    /**
     * 获取图表（带缓存）
     * 先从缓存获取，缓存不存在则从数据库获取并更新缓存
     */
    @Override
    public Chart getById(long id) {
        // 先从缓存获取
        Chart cachedChart = redisCacheManager.getCachedChart(id);
        if (cachedChart != null) {
            log.debug("从缓存获取图表: chartId={}", id);
            return cachedChart;
        }

        // 缓存不存在，从数据库获取
        Chart chart = super.getById(id);
        if (chart != null) {
            // 更新缓存
            redisCacheManager.cacheChart(chart);
            log.debug("从数据库获取并缓存图表: chartId={}", id);
        }
        return chart;
    }

    /**
     * 更新图表（同步缓存）
     */
    @Override
    public boolean updateById(Chart entity) {
        boolean result = super.updateById(entity);
        if (result && entity != null && entity.getId() != null) {
            // 更新缓存
            redisCacheManager.cacheChart(entity);
            log.debug("更新图表并同步缓存: chartId={}", entity.getId());
        }
        return result;
    }

    /**
     * 删除图表（删除缓存）
     */
    @Override
    public boolean removeById(long id) {
        // 先删除缓存
        redisCacheManager.removeChartCache(id);
        log.debug("删除图表前先删除缓存: chartId={}", id);
        
        return super.removeById(id);
    }

    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();

        // 拼接查询条件
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(userId != null && userId > 0, "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.like(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.orderByDesc("createTime");
        return queryWrapper;
    }
}
