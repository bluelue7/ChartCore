package com.chartflow.core.manager;

import com.chartflow.core.model.vo.AiResponse;
import com.chartflow.core.service.LocalAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用于对接 AI 平台
 */
@Service
@Slf4j
public class AiManager {

    @Resource
    private LocalAiService localAiService;


    /**
     * 调用本地AI
     */
    public String doChat(String message) {
        return localAiService.doChat(message);
    }

    /**
     * 专门用于图表生成的方法
     */
    public String doChartChat(String goal, String chartType, String csvData) {
        return localAiService.doChartChat(goal, chartType, csvData);
    }

    /**
     * 专门用于图表生成的方法（支持自定义prompt）
     */
    public String doChartChat(String goal,String chartType, String csvData, String customPrompt) {
        return localAiService.doChartChat(goal, chartType, csvData, customPrompt);
    }

    /**
     * 调用本地AI（返回详细信息）
     */
    public AiResponse doChatWithInfo(String message) {
        return localAiService.doChatWithInfo(message);
    }

    /**
     * 专门用于图表生成的方法（返回详细信息）
     */
    public AiResponse doChartChatWithInfo(String goal, String chartType, String csvData) {
        return localAiService.doChartChatWithInfo(goal, chartType, csvData);
    }

    /**
     * 专门用于图表生成的方法（支持自定义prompt，返回详细信息）
     */
    public AiResponse doChartChatWithInfo(String goal, String chartType, String csvData, String customPrompt) {
        return localAiService.doChartChatWithInfo(goal, chartType, csvData, customPrompt);
    }
}
