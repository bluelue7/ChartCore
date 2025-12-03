package com.chartflow.core.service;


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
}
