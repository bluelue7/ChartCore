package com.chartflow.core.utils;

import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.service.ChartImageService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailContentBuilder {

    private static ChartImageService chartImageService;

    /**
     * 设置 ChartImageService（用于生成图表图片）
     */
    public static void setChartImageService(ChartImageService service) {
        chartImageService = service;
    }

    public static String buildChartNotificationEmail(Chart chart) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n<title>图表生成完成</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Microsoft YaHei', sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }\n");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; }\n");
        html.append(".content { background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; }\n");
        html.append(".chart-box { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; }\n");
        html.append(".result-box { background: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; }\n");
        html.append("h1 { margin: 0; }\n");
        html.append("h2 { color: #333; }\n");
        html.append(".chart-container { width: 100%; }\n");
        html.append(".chart-image { width: 100%; max-width: 800px; height: auto; }\n");
        html.append("</style>\n</head>\n<body>\n");
        
        html.append("<div class=\"header\">\n<h1>📊 图表生成完成通知</h1>\n</div>\n");
        html.append("<div class=\"content\">\n");
        
        html.append("<div class=\"chart-box\">\n<h2>图表信息</h2>\n");
        html.append("<p><strong>名称：</strong>").append(escapeHtml(chart.getName())).append("</p>\n");
        html.append("<p><strong>目标：</strong>").append(escapeHtml(chart.getGoal())).append("</p>\n");
        
        if (chart.getGenChart() != null) {
            html.append("<h2>图表预览</h2>\n");
            html.append("<div class=\"chart-container\">\n");
            
            // 尝试生成图片
            String chartUrl = generateChartImage(chart.getGenChart());
            if (chartUrl != null) {
                html.append("<img class=\"chart-image\" src=\"").append(chartUrl).append("\" alt=\"图表预览\" />\n");
            } else {
                // 备用方案：显示图表配置（调试用）
                html.append("<p>图表图片生成失败，请至系统中查看图片</p>\n");
            }
            
            html.append("</div>\n");
        }
        html.append("</div>\n");
        
        html.append("<div class=\"result-box\">\n<h2>📝 分析结论</h2>\n");
        html.append("<p>").append(formatResult(chart.getGenResult())).append("</p>\n");
        html.append("</div>\n");
        
        html.append("<p style=\"text-align:center;color:#999;font-size:14px;\">图表ID: ").append(chart.getId()).append("</p>\n");
        html.append("</div>\n</body>\n</html>");
        
        return html.toString();
    }

    /**
     * 生成图表图片并上传到 OSS
     */
    private static String generateChartImage(String chartOption) {
        if (chartImageService == null) {
            log.warn("ChartImageService 未设置，无法生成图表图片");
            return null;
        }
        
        try {
            // 清理图表配置
            String cleanOption = cleanChartOption(chartOption);
            return chartImageService.generateAndUploadChart(cleanOption);
        } catch (Exception e) {
            log.error("生成图表图片失败", e);
            return null;
        }
    }

    private static String cleanChartOption(String option) {
        if (option == null) return "";
        
        String cleaned = option.replace("```javascript", "").replace("```", "").trim();
        
        if (cleaned.startsWith("option = ")) {
            cleaned = cleaned.substring(10).trim();
        }
        
        if (!cleaned.startsWith("{")) {
            int start = cleaned.indexOf("{");
            if (start != -1) {
                cleaned = cleaned.substring(start);
            }
        }
        
        return cleaned;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatResult(String result) {
        return result == null ? "暂无结论" : result.replace("\n", "<br/>");
    }
}