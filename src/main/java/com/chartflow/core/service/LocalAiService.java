package com.chartflow.core.service;

import com.chartflow.core.model.vo.AiResponse;

public interface LocalAiService {

    /**
     * 调用本地Ollama进行对话
     */
    String doChat(String message);

    /**
     * 专门为图表生成优化的方法
     */
    String doChartChat(String goal, String csvData);

    /**
     * 测试连接
     */
    boolean testConnection();

    /**
     * 调用本地Ollama进行对话（返回详细信息）
     */
    AiResponse doChatWithInfo(String message);

    /**
     * 专门为图表生成优化的方法（返回详细信息）
     */
    AiResponse doChartChatWithInfo(String goal, String csvData);
}
