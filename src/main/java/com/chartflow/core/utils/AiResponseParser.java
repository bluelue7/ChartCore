package com.chartflow.core.utils;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chartflow.core.model.entity.SeriesData; 
import com.chartflow.core.model.entity.AIResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 响应解析工具类
 * 支持多种解析策略，提高解析成功率
 */
@Slf4j
public class AiResponseParser {

    /**
     * 默认分隔符
     */
    private static final String DEFAULT_DELIMITER = "【【【【【";

    /**
     * 解析 AI 响应
     * @param content AI 返回的原始内容
     * @return 长度为2的数组，[0]是图表配置，[1]是分析结果；解析失败返回null
     */
    public static String[] parse(String content) {
        return parse(content, DEFAULT_DELIMITER);
    }

    /**
     * 解析 AI 响应（带自定义分隔符）
     */
    public static String[] parse(String content, String delimiter) {
        if (StringUtils.isBlank(content)) {
            log.warn("AI响应内容为空");
            return null;
        }

        // 策略1：按分隔符分割（最常用）
        String[] result = parseByDelimiter(content, delimiter);
        if (result != null && isValidResult(result)) {
            log.debug("使用分隔符解析成功");
            return result;
        }

        // 策略2：正则匹配JSON对象
        result = parseByRegex(content);
        if (result != null && isValidResult(result)) {
            log.debug("使用正则解析成功");
            return result;
        }

        // 策略3：查找第一个完整JSON对象
        result = parseFirstJsonObject(content);
        if (result != null && isValidResult(result)) {
            log.debug("使用JSON提取解析成功");
            return result;
        }

        // 策略4：尝试提取包含echarts关键字的内容
        result = parseByEchartsKeyword(content);
        if (result != null && isValidResult(result)) {
            log.debug("使用echarts关键字解析成功");
            return result;
        }

        log.warn("所有解析策略均失败，原始内容长度: {}", content.length());
        return null;
    }

    /**
     * 策略1：按分隔符分割
     */
    private static String[] parseByDelimiter(String content, String delimiter) {
        String[] splits = content.split(java.util.regex.Pattern.quote(delimiter));
        if (splits.length >= 3) {
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            
            // 清理可能的markdown代码块标记
            genChart = cleanCodeBlock(genChart);
            
            if (isValidChartConfig(genChart) && StringUtils.isNotBlank(genResult)) {
                return new String[]{genChart, genResult};
            }
        }
        return null;
    }

    /**
     * 策略2：正则匹配
     */
    private static String[] parseByRegex(String content) {
        // 匹配 {"title"...} 格式的JSON对象
        Pattern jsonPattern = Pattern.compile("\\{\\s*[\"']title[\"']\\s*:.*?(?=\\}\")", Pattern.DOTALL);
        Matcher matcher = jsonPattern.matcher(content);
        
        if (matcher.find()) {
            String chartConfig = matcher.group();
            // 确保JSON完整
            if (!chartConfig.endsWith("}")) {
                chartConfig = chartConfig + "}";
            }
            
            chartConfig = cleanCodeBlock(chartConfig);
            
            // 提取分析结果（JSON后面的内容）
            int endIndex = matcher.end();
            String analysis = content.substring(endIndex).trim();
            analysis = cleanAnalysisText(analysis);
            
            if (isValidChartConfig(chartConfig)) {
                return new String[]{chartConfig, analysis};
            }
        }
        return null;
    }

    /**
     * 策略3：提取第一个完整JSON对象
     */
    private static String[] parseFirstJsonObject(String content) {
        int braceCount = 0;
        int startIndex = -1;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '{') {
                if (braceCount == 0) {
                    startIndex = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && startIndex != -1) {
                    String chartConfig = content.substring(startIndex, i + 1);
                    chartConfig = cleanCodeBlock(chartConfig);
                    
                    if (isValidChartConfig(chartConfig)) {
                        String analysis = content.substring(i + 1).trim();
                        analysis = cleanAnalysisText(analysis);
                        return new String[]{chartConfig, analysis};
                    }
                }
            }
        }
        return null;
    }

    /**
     * 策略4：按echarts关键字提取
     */
    private static String[] parseByEchartsKeyword(String content) {
        // 查找包含echarts特征的内容
        String lowerContent = content.toLowerCase();
        
        // 查找可能的图表配置区域
        int optionStart = lowerContent.indexOf("option");
        int jsonStart = lowerContent.indexOf("{");
        
        if (optionStart != -1 || jsonStart != -1) {
            int start = Math.min(
                optionStart != -1 ? optionStart : Integer.MAX_VALUE,
                jsonStart != -1 ? jsonStart : Integer.MAX_VALUE
            );
            
            // 从start位置开始找完整JSON
            String subContent = content.substring(start);
            return parseFirstJsonObject(subContent);
        }
        return null;
    }

    /**
     * 清理markdown代码块标记
     */
    private static String cleanCodeBlock(String content) {
        if (content == null) return null;
        
        // 移除 ```json 或 ```javascript 标记
        content = content.replaceAll("^```(json|javascript|js)?\\s*", "");
        content = content.replaceAll("\\s*```$", "");
        
        // 移除单引号包裹的情况
        if (content.startsWith("'") && content.endsWith("'")) {
            content = content.substring(1, content.length() - 1);
        }
        
        return content.trim();
    }

    /**
     * 清理分析文本
     */
    private static String cleanAnalysisText(String content) {
        if (content == null) return "";
        
        // 移除分隔符残留
        content = content.replace("【【【【【", "").trim();
        
        // 如果分析结果也被JSON化了，尝试解析
        if (content.startsWith("{") && content.endsWith("}")) {
            try {
                JSONObject obj = JSON.parseObject(content);
                // 如果是嵌套结构，提取内容
                if (obj.containsKey("content") || obj.containsKey("analysis")) {
                    return obj.getString("content") != null ? obj.getString("content") : obj.getString("analysis");
                }
            } catch (Exception ignored) {
                // 不是有效JSON，保持原样
            }
        }
        
        return content;
    }

    /**
     * 验证图表配置是否有效
     */
    private static boolean isValidChartConfig(String config) {
        if (StringUtils.isBlank(config)) {
            return false;
        }
        
        // 基本校验：必须是JSON对象格式
        if (!config.startsWith("{") || !config.endsWith("}")) {
            return false;
        }
        
        // 尝试解析JSON，验证结构
        try {
            JSONObject json = JSON.parseObject(config);
            
            // 检查echarts必需字段
            boolean hasEssentialFields = 
                json.containsKey("title") || 
                json.containsKey("xAxis") || 
                json.containsKey("yAxis") || 
                json.containsKey("series");
            
            return hasEssentialFields;
            
        } catch (Exception e) {
            log.debug("图表配置JSON解析失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证解析结果是否有效
     */
    private static boolean isValidResult(String[] result) {
        return result != null && 
               result.length >= 2 && 
               StringUtils.isNotBlank(result[0]) && 
               isValidChartConfig(result[0]);
    }

    /**
     * 验证生成的图表配置是否可用（更严格的校验）
     */
    public static boolean validateChartConfig(String config) {
        if (!isValidChartConfig(config)) {
            return false;
        }

        try {
            JSONObject json = JSON.parseObject(config);

            // 检查series是否存在且非空
            if (!json.containsKey("series")) {
                log.warn("图表配置缺少series字段");
                return false;
            }

            // 检查series是否为数组
            Object series = json.get("series");
            if (!(series instanceof java.util.List)) {
                log.warn("series字段不是数组格式");
                return false;
            }

            @SuppressWarnings("unchecked")
            java.util.List<JSONObject> seriesList = (java.util.List<JSONObject>) series;
            if (seriesList.isEmpty()) {
                log.warn("series数组为空");
                return false;
            }

            // 检查每个series是否有必需字段
            for (JSONObject s : seriesList) {
                if (!s.containsKey("type")) {
                    log.warn("series缺少type字段");
                    return false;
                }
                if (!s.containsKey("data")) {
                    log.warn("series缺少data字段");
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("图表配置校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 AI 结构化响应（AIResult 格式）
     * 用于新架构：AI 返回结构化 JSON，后端生成 ECharts 配置
     * @param content AI 返回的原始内容
     * @return AIResult 对象，解析失败返回 null
     */
    public static AIResult parseAIResult(String content) {
        if (StringUtils.isBlank(content)) {
            log.warn("AI响应内容为空");
            return null;
        }

        try {
            // 策略1：按分隔符提取 JSON
            AIResult result = extractAIResultByDelimiter(content);
            if (result != null && isValidAIResult(result)) {
                log.debug("使用分隔符解析AIResult成功");
                return result;
            }

            // 策略2：直接解析整个内容为 JSON
            result = parseDirectly(content);
            if (result != null && isValidAIResult(result)) {
                log.debug("直接解析AIResult成功");
                return result;
            }

            // 策略3：提取第一个完整的 JSON 对象
            result = extractFirstJsonAsAIResult(content);
            if (result != null && isValidAIResult(result)) {
                log.debug("提取JSON对象解析AIResult成功");
                return result;
            }

            log.warn("所有解析策略均失败，原始内容长度: {}", content.length());
            return null;

        } catch (Exception e) {
            log.error("解析AIResult失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 按分隔符提取 AIResult JSON
     */
    private static AIResult extractAIResultByDelimiter(String content) {
        String[] splits = content.split(java.util.regex.Pattern.quote(DEFAULT_DELIMITER));
        if (splits.length >= 2) {
            String jsonStr = splits[1].trim();
            jsonStr = cleanCodeBlock(jsonStr);
            return parseAsAIResult(jsonStr);
        }
        return null;
    }

    /**
     * 直接解析字符串为 AIResult
     */
    private static AIResult parseDirectly(String content) {
        String cleaned = cleanCodeBlock(content.trim());
        return parseAsAIResult(cleaned);
    }

    /**
     * 提取第一个完整 JSON 对象作为 AIResult
     */
    private static AIResult extractFirstJsonAsAIResult(String content) {
        int braceCount = 0;
        int startIndex = -1;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '{') {
                if (braceCount == 0) {
                    startIndex = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && startIndex != -1) {
                    String jsonStr = content.substring(startIndex, i + 1);
                    return parseAsAIResult(jsonStr);
                }
            }
        }
        return null;
    }

    /**
     * 将 JSON 字符串解析为 AIResult
     */
    private static AIResult parseAsAIResult(String jsonStr) {
        if (StringUtils.isBlank(jsonStr) || !jsonStr.startsWith("{")) {
            return null;
        }

        try {
            AIResult result = JSON.parseObject(jsonStr, AIResult.class);

            // 验证必要字段
            if (result == null) {
                return null;
            }

            // 检查 chartType 是否有有效值
            if (StringUtils.isBlank(result.getChartType())) {
                log.warn("AIResult 缺少 chartType 字段");
                return null;
            }

            // 检查 categories 是否有数据
            if (result.getCategories() == null || result.getCategories().isEmpty()) {
                log.warn("AIResult categories 为空");
                return null;
            }

            // 检查 series 是否有数据
            if (result.getSeries() == null || result.getSeries().isEmpty()) {
                log.warn("AIResult series 为空");
                return null;
            }

            return result;

        } catch (Exception e) {
            log.debug("JSON 解析为 AIResult 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 AIResult 是否有效
     */
    private static boolean isValidAIResult(AIResult result) {
        if (result == null) {
            return false;
        }

        if (StringUtils.isBlank(result.getChartType())) {
            log.warn("AIResult.chartType 为空");
            return false;
        }
        String chartType = result.getChartType();
        Set<String> typesNeedCategories = new HashSet<>(Arrays.asList("bar", "line", "pie"));

        // bar/line/pie 必须 categories 非空
        if (typesNeedCategories.contains(chartType)) {
            if (result.getCategories() == null || result.getCategories().isEmpty()) {
                log.warn("图表类型 {} 要求 categories 非空", chartType);
                return false;
            }
        }

        // 雷达图两种方式二选一（这里采用提示词描述：categories 作为维度名称，radarIndicator 可选）
        if ("radar".equals(chartType)) {
            if (result.getCategories() == null || result.getCategories().isEmpty()) {
                log.warn("雷达图要求 categories 非空作为维度名称");
                return false;
            }
            // radarIndicator 不强制，可以自动生成
        }

        // scatter 不要求 categories，也不需要额外检查

        if (result.getSeries() == null || result.getSeries().isEmpty()) {
            log.warn("AIResult.series 为空");
            return false;
        }

        // 验证每个 series 都有 data（兼容 SeriesData 或 JSONObject）
        for (Object s : result.getSeries()) {
            boolean hasData = false;
            if (s instanceof SeriesData) {
                hasData = ((SeriesData) s).getData() != null;
            } else if (s instanceof JSONObject) {
                hasData = ((JSONObject) s).containsKey("data");
            } else {
                // 尝试反射检查
                try {
                    Object data = s.getClass().getMethod("getData").invoke(s);
                    hasData = data != null;
                } catch (Exception ignored) {
                    hasData = false;
                }
            }
            if (!hasData) {
                log.warn("AIResult.series 缺少 data 字段");
                return false;
            }
        }
        return true;
    }
}