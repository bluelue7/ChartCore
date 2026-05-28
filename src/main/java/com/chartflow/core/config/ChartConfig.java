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
            "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                    "分析需求：\n" +
                    "{数据分析的需求或者目标}\n" +
                    "原始数据：\n" +
                    "{csv格式的原始数据，用,作为分隔符}\n" +
                    "\n" +
                    "请严格按照以下规则输出，不要输出任何多余的开头、结尾、注释或代码块标记（如```json或```）：\n" +
                    "\n" +
                    "1. 第一行必须单独输出五个左方括号：【【【【【\n" +
                    "2. 紧接着（换行后）输出一个合法的JSON对象，表示图表配置。JSON必须符合ECharts格式，所有键名使用双引号，不能有注释或多余逗号。\n" +
                    "3. JSON输出完毕后，必须另起一行单独输出五个左方括号：【【【【【\n" +
                    "4. 然后输出详细的数据分析结论，纯文本，不要加引号或JSON结构。\n" +
                    "5. 整个输出中不要包含任何其他文字（例如“好的”、“根据分析”、“以下是结果”等）。\n" +
                    "6. 根据数据特点和分析需求选择合适的 ECharts 图表类型，不要局限于示例中的折线图。\n"+
                    "\n" +
                    "【输出示例】（严格复制此结构，但不要输出示例中的注释）：\n" +
                    "【【【【【\n" +
                    "{\"title\":{\"text\":\"销售额趋势\"},\"xAxis\":{\"name\":\"日期\"},\"yAxis\":{\"name\":\"销售额\"},\"series\":[{\"name\":\"电子产品\",\"type\":\"line\",\"data\":[50000,55000]}]}\n" +
                    "【【【【【\n" +
                    "从数据可以看出，电子产品销售额持续增长，最高达到60000元；服装类销售额相对平稳；建议重点投入电子产品推广。\n" +
                    "\n" +
                    "现在请根据下面提供的内容生成输出。\n" +
                    "====================\n" +
                    "分析需求：\n" +
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