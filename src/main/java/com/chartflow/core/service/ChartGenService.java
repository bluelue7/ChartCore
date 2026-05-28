package com.chartflow.core.service;

import com.chartflow.core.model.dto.chart.GenChartByAiRequest;
import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.model.entity.User;
import com.chartflow.core.model.vo.AiResponse;
import com.chartflow.core.model.vo.BiResponse;
import com.chartflow.core.model.vo.ChartGenResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图表生成服务接口
 */
public interface ChartGenService {

    /**
     * 同步生成图表
     *
     * @param file    上传的文件
     * @param request 请求参数
     * @param user    当前用户
     * @return 图表响应
     */
    BiResponse generateChartSync(MultipartFile file, GenChartByAiRequest request, User user);

    /**
     * 异步生成图表（线程池）
     *
     * @param file    上传的文件
     * @param request 请求参数
     * @param user    当前用户
     * @return 图表ID
     */
    Long generateChartAsync(MultipartFile file, GenChartByAiRequest request, User user);

    /**
     * 异步生成图表（MQ）
     *
     * @param file    上传的文件
     * @param request 请求参数
     * @param user    当前用户
     * @return 图表ID
     */
    Long generateChartAsyncMq(MultipartFile file, GenChartByAiRequest request, User user);

    /**
     * MQ消息处理：执行图表生成（带重试机制）
     * 当AI响应解析失败时，自动重新调用AI最多重试2次
     *
     * @param chartId  图表ID
     * @param promptId Prompt ID（可为null）
     * @return 处理结果，包含genChart和genResult
     */
    ChartGenResult processChartGenTask(Long chartId, Long promptId);

    /**
     * 带重试机制的AI调用
     * 当解析失败时自动重试，最多重试2次
     *
     * @param goal       分析目标
     * @param chartType  图表类型
     * @param csvData    CSV数据
     * @param promptQuery 提示词（可为null）
     * @return AI响应，如果重试后仍失败返回null
     */
    AiResponse generateWithRetry(String goal, String chartType, String csvData, String promptQuery);

    /**
     * 获取默认图表配置（当AI生成失败时使用）
     *
     * @param goal    目标
     * @param csvData CSV数据
     * @return 默认图表配置JSON
     */
    String getDefaultChartConfig(String goal, String csvData);

    /**
     * 尝试修复图表配置JSON
     *
     * @param config 原始配置
     * @return 修复后的配置，如果修复失败返回原配置
     */
    String tryFixChartConfig(String config);
}