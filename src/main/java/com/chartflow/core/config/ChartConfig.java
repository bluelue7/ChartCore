package com.chartflow.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 图表生成配置类
 */
@Configuration
@ConfigurationProperties(prefix = "chart")
@Data
public class ChartConfig {

    /**
     * 默认 Prompt 模板
     */
    private String defaultPrompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
            "分析需求：\n" +
            "{数据分析的需求或者目标}\n" +
            "原始数据：\n" +
            "{csv格式的原始数据，用,作为分隔符}\n" +
            "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
            "【【【【【\n" +
            "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释" +
            "【【【【【\n" +
            "{明确的数据分析结论、越详细越好，不要生成多余的注释，比如注释}";

    /**
     * AI 响应分隔符
     */
    private String responseDelimiter = "【【【【【";

    /**
     * 最大重试次数（异步任务）
     */
    private int maxRetryCount = 30;

    /**
     * 重试间隔（毫秒）
     */
    private long retryIntervalMs = 2000;
}