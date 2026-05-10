package com.chartflow.core.utils;

import com.chartflow.core.model.entity.Chart;

public class EmailContentBuilder {

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
        html.append(".chart-container { width: 100%; height: 400px; }\n");
        html.append("</style>\n</head>\n<body>\n");
        
        html.append("<div class=\"header\">\n<h1>📊 图表生成完成通知</h1>\n</div>\n");
        html.append("<div class=\"content\">\n");
        
        html.append("<div class=\"chart-box\">\n<h2>图表信息</h2>\n");
        html.append("<p><strong>名称：</strong>").append(escapeHtml(chart.getName())).append("</p>\n");
        html.append("<p><strong>目标：</strong>").append(escapeHtml(chart.getGoal())).append("</p>\n");
        
        if (chart.getGenChart() != null) {
            html.append("<h2>图表预览</h2>\n");
            html.append("<div class=\"chart-container\">\n");
            html.append("<script src=\"https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js\"></script>\n");
            html.append("<div id=\"chart\" style=\"width:100%;height:100%;\"></div>\n");
            html.append("<script>var chart=echarts.init(document.getElementById('chart'));var option=").append(chart.getGenChart()).append(";chart.setOption(option);</script>\n");
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

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatResult(String result) {
        return result == null ? "暂无结论" : result.replace("\n", "<br/>");
    }
}
