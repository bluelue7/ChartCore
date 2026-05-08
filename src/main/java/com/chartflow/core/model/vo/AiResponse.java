package com.chartflow.core.model.vo;

import lombok.Data;

/**
 * AI响应结果，包含内容和Token信息
 */
@Data
public class AiResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 输入Token数量
     */
    private Integer inputTokens;

    /**
     * 输出Token数量
     */
    private Integer outputTokens;

    /**
     * 总Token数量
     */
    private Integer totalTokens;

    /**
     * 创建成功响应
     */
    public static AiResponse success(String content) {
        AiResponse response = new AiResponse();
        response.setContent(content);
        return response;
    }

    /**
     * 创建成功响应（带Token信息）
     */
    public static AiResponse success(String content, Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        AiResponse response = new AiResponse();
        response.setContent(content);
        response.setInputTokens(inputTokens);
        response.setOutputTokens(outputTokens);
        response.setTotalTokens(totalTokens);
        return response;
    }
}
