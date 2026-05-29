package com.chartflow.core.factory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chartflow.core.model.entity.AIResult;
import com.chartflow.core.model.entity.SeriesData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 图表工厂
 * 负责将 AI 分析结果转换为 ECharts 配置
 * AI 只负责分析数据，后端统一生成图表配置
 */
@Slf4j
public class ChartFactory {

    /**
     * 根据 AI 分析结果生成 ECharts 配置
     */
    public static JSONObject buildOption(AIResult result) {
        if (result == null) {
            log.error("AIResult 为空，无法生成图表配置");
            return new JSONObject();
        }

        String chartType = result.getChartType();
        if (chartType == null) {
            chartType = "bar";
        }

        switch (chartType.toLowerCase()) {
            case "line":
                return buildLineChart(result);
            case "pie":
                return buildPieChart(result);
            case "scatter":
                return buildScatterChart(result);
            case "radar":
                return buildRadarChart(result);
            case "bar":
            default:
                return buildBarChart(result);
        }
    }

    /**
     * 生成柱状图配置
     */
    private static JSONObject buildBarChart(AIResult result) {
        JSONObject option = new JSONObject();

        JSONObject title = new JSONObject();
        title.put("text", result.getTitle());
        title.put("left", "center");
        title.put("top", 20);
        title.put("textStyle", createTextStyle());
        option.put("title", title);

        option.put("tooltip", createTooltip());

        option.put("legend", createLegend(result.getSeries()));

        JSONObject xAxis = new JSONObject();
        xAxis.put("type", "category");
        xAxis.put("data", result.getCategories());
        xAxis.put("axisLabel", createAxisLabel());
        xAxis.put("axisLine", createAxisLine());
        option.put("xAxis", xAxis);

        JSONObject yAxis = new JSONObject();
        yAxis.put("type", "value");
        yAxis.put("axisLabel", createAxisLabel());
        yAxis.put("axisLine", createAxisLine());
        option.put("yAxis", yAxis);

        option.put("series", createSeriesList(result.getSeries(), "bar"));
        option.put("grid", createGrid());

        return option;
    }

    /**
     * 生成折线图配置
     */
    private static JSONObject buildLineChart(AIResult result) {
        JSONObject option = new JSONObject();

        JSONObject title = new JSONObject();
        title.put("text", result.getTitle());
        title.put("left", "center");
        title.put("top", 20);
        title.put("textStyle", createTextStyle());
        option.put("title", title);

        option.put("tooltip", createTooltip());

        option.put("legend", createLegend(result.getSeries()));

        JSONObject xAxis = new JSONObject();
        xAxis.put("type", "category");
        xAxis.put("data", result.getCategories());
        xAxis.put("axisLabel", createAxisLabel());
        xAxis.put("axisLine", createAxisLine());
        xAxis.put("boundaryGap", false);
        option.put("xAxis", xAxis);

        JSONObject yAxis = new JSONObject();
        yAxis.put("type", "value");
        yAxis.put("axisLabel", createAxisLabel());
        yAxis.put("axisLine", createAxisLine());
        option.put("yAxis", yAxis);

        option.put("series", createSeriesList(result.getSeries(), "line"));

        option.put("grid", createGrid());

        return option;
    }

    /**
     * 生成饼图配置
     */
    private static JSONObject buildPieChart(AIResult result) {
        JSONObject option = new JSONObject();

        JSONObject title = new JSONObject();
        title.put("text", result.getTitle());
        title.put("left", "center");
        title.put("top", 20);
        title.put("textStyle", createTextStyle());
        option.put("title", title);

        option.put("tooltip", createTooltip());

        JSONArray seriesArray = new JSONArray();
        if (result.getSeries() != null && !result.getSeries().isEmpty()) {
            SeriesData seriesData = result.getSeries().get(0);
            JSONObject series = new JSONObject();
            series.put("name", seriesData.getName());
            series.put("type", "pie");
            series.put("radius", "50%");
            series.put("center", new JSONArray().fluentAdd("50%").fluentAdd("50%"));
            series.put("data", createPieData(seriesData.getData(), result.getCategories()));
            series.put("emphasis", createEmphasis());
            seriesArray.add(series);
        }
        option.put("series", seriesArray);

        return option;
    }

    /**
     * 生成散点图配置
     * 数据格式校验：series[].data 必须是 [[x1,y1], [x2,y2], ...] 格式
     */
    private static JSONObject buildScatterChart(AIResult result) {
        JSONObject option = new JSONObject();

        JSONObject title = new JSONObject();
        title.put("text", result.getTitle());
        title.put("left", "center");
        title.put("top", 20);
        title.put("textStyle", createTextStyle());
        option.put("title", title);

        option.put("tooltip", createTooltip());

        option.put("legend", createLegend(result.getSeries()));

        JSONObject xAxis = new JSONObject();
        xAxis.put("type", "value");
        xAxis.put("axisLabel", createAxisLabel());
        xAxis.put("axisLine", createAxisLine());
        option.put("xAxis", xAxis);

        JSONObject yAxis = new JSONObject();
        yAxis.put("type", "value");
        yAxis.put("axisLabel", createAxisLabel());
        yAxis.put("axisLine", createAxisLine());
        option.put("yAxis", yAxis);

        // 校验并转换散点图数据
        JSONArray validatedSeries = validateAndConvertScatterData(result.getSeries());
        option.put("series", validatedSeries);
        option.put("grid", createGrid());

        return option;
    }

    /**
     * 生成雷达图配置
     * 优先使用 AIResult.radarIndicator，为空时根据 categories 自动生成
     */
    private static JSONObject buildRadarChart(AIResult result) {
        JSONObject option = new JSONObject();

        JSONObject title = new JSONObject();
        title.put("text", result.getTitle());
        title.put("left", "center");
        title.put("top", 20);
        title.put("textStyle", createTextStyle());
        option.put("title", title);

        option.put("tooltip", createTooltip());

        JSONObject radar = new JSONObject();
        // 优先使用 AIResult.radarIndicator
        List<AIResult.RadarIndicator> radarIndicators = result.getRadarIndicator();
        List<String> categories = result.getCategories();
        if (radarIndicators != null && !radarIndicators.isEmpty()) {
            // 使用 AI 提供的 radarIndicator
            JSONArray indicator = new JSONArray();
            for (AIResult.RadarIndicator ri : radarIndicators) {
                JSONObject ind = new JSONObject();
                ind.put("name", ri.getName());
                ind.put("max", ri.getMax() != null ? ri.getMax() : 100);
                ind.put("min", ri.getMin() != null ? ri.getMin() : 0);
                indicator.add(ind);
            }
            radar.put("indicator", indicator);
        } else if (categories != null) {
            // 自动根据 categories 生成 indicator
            JSONArray indicator = new JSONArray();
            for (String category : categories) {
                JSONObject ind = new JSONObject();
                ind.put("name", category);
                ind.put("max", 100);
                indicator.add(ind);
            }
            radar.put("indicator", indicator);
        }
        radar.put("center", new JSONArray().fluentAdd("50%").fluentAdd("55%"));
        radar.put("radius", "60%");
        option.put("radar", radar);
        option.put("series", createSeriesList(result.getSeries(), "radar"));

        return option;
    }

    private static JSONObject createTooltip() {
        JSONObject tooltip = new JSONObject();
        tooltip.put("trigger", "axis");
        tooltip.put("axisPointer", new JSONObject().fluentPut("type", "cross"));
        return tooltip;
    }

    private static JSONObject createLegend(List<SeriesData> seriesList) {
        JSONObject legend = new JSONObject();
        legend.put("top", 60);
        if (seriesList != null && seriesList.size() > 1) {
            JSONArray data = new JSONArray();
            for (SeriesData series : seriesList) {
                data.add(series.getName());
            }
            legend.put("data", data);
        }
        return legend;
    }

    private static JSONObject createAxisLabel() {
        JSONObject axisLabel = new JSONObject();
        axisLabel.put("color", "#666");
        return axisLabel;
    }

    private static JSONObject createAxisLine() {
        JSONObject axisLine = new JSONObject();
        axisLine.put("lineStyle", new JSONObject().fluentPut("color", "#ccc"));
        return axisLine;
    }

    private static JSONObject createTextStyle() {
        JSONObject textStyle = new JSONObject();
        textStyle.put("fontSize", 16);
        textStyle.put("fontWeight", "bold");
        textStyle.put("color", "#333");
        return textStyle;
    }

    private static JSONObject createGrid() {
        JSONObject grid = new JSONObject();
        grid.put("left", "10%");
        grid.put("right", "10%");
        grid.put("bottom", "15%");
        grid.put("top", "25%");
        grid.put("containLabel", true);
        return grid;
    }

    private static JSONObject createEmphasis() {
        JSONObject emphasis = new JSONObject();
        emphasis.put("itemStyle", new JSONObject().fluentPut("shadowBlur", 10).fluentPut("shadowOffsetX", 0).fluentPut("shadowColor", "rgba(0, 0, 0, 0.5)"));
        return emphasis;
    }

    private static JSONArray createSeriesList(List<SeriesData> seriesList, String type) {
        JSONArray seriesArray = new JSONArray();
        if (seriesList == null) {
            return seriesArray;
        }

        for (SeriesData seriesData : seriesList) {
            JSONObject series = new JSONObject();
            series.put("name", seriesData.getName());
            series.put("type", type);
            series.put("data", seriesData.getData());

            if ("line".equals(type)) {
                series.put("smooth", true);
                series.put("areaStyle", new JSONObject().fluentPut("opacity", 0.3));
            }

            seriesArray.add(series);
        }
        return seriesArray;
    }

    private static JSONArray createPieData(List<Object> data, List<String> categories) {
        JSONArray pieData = new JSONArray();
        if (data != null && categories != null) {
            for (int i = 0; i < Math.min(data.size(), categories.size()); i++) {
                JSONObject item = new JSONObject();
                item.put("name", categories.get(i));
                item.put("value", data.get(i));
                pieData.add(item);
            }
        }
        return pieData;
    }

    /**
     * 校验并转换散点图数据格式
     * 要求：series[].data 必须是 [[x1,y1], [x2,y2], ...] 格式
     * @return 校验后的系列数据列表
     */
    private static JSONArray validateAndConvertScatterData(List<SeriesData> seriesList) {
        JSONArray validatedSeries = new JSONArray();
        if (seriesList == null) {
            return validatedSeries;
        }

        for (SeriesData seriesData : seriesList) {
            JSONObject series = new JSONObject();
            series.put("name", seriesData.getName());
            series.put("type", "scatter");

            List<Object> originalData = seriesData.getData();
            JSONArray validatedData = new JSONArray();

            if (originalData != null) {
                for (Object item : originalData) {
                    // 每个数据点必须是 [x, y] 格式的数组或列表
                    if (item instanceof List) {
                        List<?> point = (List<?>) item;
                        if (point.size() >= 2) {
                            JSONArray validatedPoint = new JSONArray();
                            validatedPoint.add(point.get(0));
                            validatedPoint.add(point.get(1));
                            validatedData.add(validatedPoint);
                        } else {
                            log.warn("散点图数据点格式错误：维度不足，已忽略: {}", item);
                        }
                    } else if (item instanceof Object[]) {
                        Object[] point = (Object[]) item;
                        if (point.length >= 2) {
                            JSONArray validatedPoint = new JSONArray();
                            validatedPoint.add(point[0]);
                            validatedPoint.add(point[1]);
                            validatedData.add(validatedPoint);
                        } else {
                            log.warn("散点图数据点格式错误：维度不足，已忽略: {}", item);
                        }
                    } else {
                        log.warn("散点图数据格式错误：期望 [[x,y], [x,y], ...] 格式，实际收到: {}", item);
                    }
                }
            }

            series.put("data", validatedData);
            validatedSeries.add(series);
        }
        return validatedSeries;
    }
}