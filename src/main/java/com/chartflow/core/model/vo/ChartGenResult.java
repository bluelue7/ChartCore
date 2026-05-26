package com.chartflow.core.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图表生成结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartGenResult {
    
    /**
     * 生成的图表配置（ECharts JSON）
     */
    private String genChart;
    
    /**
     * 生成的分析结果
     */
    private String genResult;
    
    /**
     * 处理状态：success / partial / failed
     */
    private String status;
    
    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
    
    /**
     * AI原始响应（如果有）
     */
    private String aiRawResponse;
    
    /**
     * Token使用信息
     */
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    
    public static ChartGenResult success(String genChart, String genResult, AiResponse aiResponse) {
        ChartGenResult result = new ChartGenResult();
        result.setGenChart(genChart);
        result.setGenResult(genResult);
        result.setStatus("success");
        if (aiResponse != null) {
            result.setInputTokens(aiResponse.getInputTokens());
            result.setOutputTokens(aiResponse.getOutputTokens());
            result.setTotalTokens(aiResponse.getTotalTokens());
        }
        return result;
    }
    
    public static ChartGenResult partial(String genChart, String genResult, String errorMessage) {
        ChartGenResult result = new ChartGenResult();
        result.setGenChart(genChart);
        result.setGenResult(genResult);
        result.setStatus("partial");
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    public static ChartGenResult failed(String errorMessage) {
        ChartGenResult result = new ChartGenResult();
        result.setStatus("failed");
        result.setErrorMessage(errorMessage);
        return result;
    }
}