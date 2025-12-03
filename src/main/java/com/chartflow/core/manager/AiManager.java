package com.chartflow.core.manager;

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
    public String doChartChat(String goal, String csvData) {
        return localAiService.doChartChat(goal, csvData);
    }
}
