package com.chartflow.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图表生成配置类
 * 使用 @Component 替代 @Configuration，避免与 @ConfigurationProperties 冲突
 */
@Component
@ConfigurationProperties(prefix = "chart")
@Data
public class ChartConfig {

    /**
     * 默认 Prompt 模板
     */
//    private String defaultPrompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
//            "分析需求：\n" +
//            "{数据分析的需求或者目标}\n" +
//            "原始数据：\n" +
//            "{csv格式的原始数据，用,作为分隔符}\n" +
//            "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
//            "【【【【【\n" +
//            "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释" +
//            "【【【【【\n" +
//            "{明确的数据分析结论、越详细越好，不要生成多余的注释，比如注释}";

        private String defaultPrompt =
            "你是一个专业的数据分析助手。\n" +
                    "用户会提供：\n" +
                    "1. 分析需求\n" +
                    "2. 用户指定的图表类型（可为空）\n" +
                    "3. CSV格式数据\n" +
                    "\n" +
                    "你的任务：\n" +
                    "1. 识别最适合的数据可视化类型\n" +
                    "2. 提取核心分析字段\n" +
                    "3. 输出标准JSON\n" +
                    "4. 给出数据分析结论\n" +
                    "\n" +
                    "【重要规则】\n" +
                    "1. 只能输出合法JSON\n" +
                    "2. 不要输出 markdown\n" +
                    "3. 不要输出 ``` json 或 ```\n" +
                    "4. 不要输出解释文字\n" +
                    "5. 所有 key 必须使用双引号\n" +
                    "6. 不允许出现注释\n" +
                    "7. 不允许出现 function\n" +
                    "8. 不允许输出 ECharts option\n" +
                    "9. 输出必须可以被 Jackson/Fastjson 直接解析\n" +
                    
                    "\n" +
                    "【固定输出格式】\n" +
                    "【【【【【\n" +
                    "{\"chartType\":\"bar\",\"title\":\"标题\",\"xField\":\"X轴字段名\",\"yField\":\"Y轴字段名\",\"categories\":[\"分类1\",\"分类2\"],\"series\":[{\"name\":\"系列名称\",\"data\":[100,200]}],\"conclusion\":\"数据分析结论\"}\n" +
                    "【【【【【\n" +
                    "\n" +
                    "【chartType规则】\n" +
                    "1. 如果用户指定了 chartType（如 bar/line/pie/scatter/radar/stack）：\n" +
                    "   - 必须严格使用用户指定的值\n" +
                    "   - 不允许修改\n" +
                    "   - 不允许自行选择其他图表类型\n" +
                    "2. 如果用户没有指定 chartType：\n" +
                    "   - 根据数据特征和分析需求自动选择最合适的图表类型\n" +
                    "\n" +
                    "【chartType允许值】\n" +
                    "- bar（分类对比）\n" +
                    "- line（时间趋势）\n" +
                    "- pie（占比分析）\n" +
                    "- scatter（相关性分析）\n" +
                    "- radar（多维指标）\n" +
                    "- stack（堆叠图，用于展示多组数据累积对比）\n" +
                    "\n" +
                    "现在开始分析：\n" +
                    "====================\n" +
                    "分析需求：\n" +
                    "%s\n" +
                    "用户指定的图表类型：\n" +
                    "%s\n" +
                    "原始数据：\n" +
                    "%s";

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