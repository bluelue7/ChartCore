package com.chartflow.core.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 分析结果
 * AI 只负责分析数据，返回结构化的分析元数据
 * 后端 ChartFactory 根据此数据生成 ECharts 配置
 */
@Data
public class AIResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图表类型
     * bar / line / pie / scatter / radar / stack
     */
    private String chartType;

    /**
     * 图表标题
     */
    private String title;

    /**
     * X轴字段名
     */
    private String xField;

    /**
     * Y轴字段名
     */
    private String yField;

    /**
     * 分类数据（X轴数据）
     */
    private List<String> categories;

    /**
     * 系列数据
     */
    private List<SeriesData> series;

    /**
     * AI 分析结论
     */
    private String conclusion;

    /**
     * 雷达图指标配置（仅当 chartType = "radar" 时使用）
     * 如果未提供，系统会根据 categories 自动生成
     */
    private List<RadarIndicator> radarIndicator;

    /**
     * 雷达图指标配置类
     */
    @Data
    public static class RadarIndicator implements Serializable {
        private static final long serialVersionUID = 1L;
        /**
         * 指标名称（通常与 categories 对应）
         */
        private String name;
        /**
         * 可选，最大值，默认自动计算
         */
        private Double max;
        /**
         * 可选，最小值，默认 0
         */
        private Double min;
    }
}