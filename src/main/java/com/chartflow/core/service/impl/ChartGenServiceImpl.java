package com.chartflow.core.service.impl;

import com.chartflow.core.bizmq.BiMessageProducer;
import com.chartflow.core.config.ChartConfig;
import com.chartflow.core.constant.UserConstant;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.exception.ThrowUtils;
import com.chartflow.core.manager.AiManager;
import com.chartflow.core.manager.RedisLimiterManager;
import com.chartflow.core.model.dto.chart.GenChartByAiRequest;
import com.chartflow.core.model.dto.modelrecord.ModelRecordAddRequest;
import com.chartflow.core.model.dto.tasklog.TaskLogAddRequest;
import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.model.entity.ModelRecord;
import com.chartflow.core.model.entity.Prompt;
import com.chartflow.core.model.entity.User;
import com.chartflow.core.model.vo.AiResponse;
import com.chartflow.core.model.vo.BiResponse;
import com.chartflow.core.model.vo.ChartGenResult;
import com.chartflow.core.service.*;
import com.chartflow.core.utils.AiResponseParser;
import com.chartflow.core.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表生成服务实现
 * 将图表生成的核心业务逻辑从 Controller 抽取出来
 * 支持同步生成、异步线程池生成、MQ异步生成三种模式
 */
@Service
@Slf4j
public class ChartGenServiceImpl implements ChartGenService {

    /**
     * 文件大小限制（50MB）
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;

    /**
     * 允许的文件后缀
     */
    private static final List<String> VALID_FILE_SUFFIX_LIST = List.of("xlsx", "xls");

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 2;

    /**
     * 初始重试间隔（毫秒）
     */
    private static final long RETRY_INTERVAL_MS = 1000;

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private PromptService promptService;

    @Resource
    private ModelRecordService modelRecordService;

    @Resource
    private TaskLogService taskLogService;

    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private ChartConfig chartConfig;

    @Override
    public BiResponse generateChartSync(MultipartFile file, GenChartByAiRequest request, User user) {
        // 参数校验
        validateRequest(request, file);
        
        // 限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" + user.getId());

        // 获取 Prompt
        String promptQuery = getPrompt(request.getPromptId());

        // 解析 Excel 数据
        String csvData = ExcelUtils.excelToCsv(file);

        // 构建用户目标
        String userGoal = buildUserGoal(request.getGoal(), request.getChartType());

        // 构建完整请求内容
        String fullRequestContent = buildFullRequestContent(promptQuery, userGoal, csvData, request.getPromptId());

        long startTime = System.currentTimeMillis();
        Long modelRecordId = null;

        try {
            // 插入模型调用记录
            modelRecordId = createModelRecord(user.getId(), fullRequestContent);

            // 调用 AI 生成图表（带重试）
            AiResponse aiResponse = generateWithRetry(userGoal, csvData, promptQuery);
            
            if (aiResponse == null) {
                // 重试失败，使用默认配置
                String defaultConfig = getDefaultChartConfig(userGoal, csvData);
                String defaultAnalysis = "AI生成失败，使用默认图表配置。";
                
                // 更新模型调用记录
                int costMs = (int) (System.currentTimeMillis() - startTime);
                modelRecordService.updateModelRecordStatus(modelRecordId, "partial", null, "AI生成失败", costMs);
                
                // 保存图表
                Chart chart = saveChart(request.getName(), request.getGoal(), csvData, 
                        request.getChartType(), defaultConfig, defaultAnalysis, user.getId());
                
                modelRecordService.updateChartId(modelRecordId, chart.getId());
                
                BiResponse biResponse = new BiResponse();
                biResponse.setGenChart(defaultConfig);
                biResponse.setGenResult(defaultAnalysis);
                biResponse.setChartId(chart.getId());
                return biResponse;
            }

            // 解析 AI 响应
            String[] parts = AiResponseParser.parse(aiResponse.getContent());
            if (parts == null) {
                // 解析失败，尝试修复或使用默认配置
                String fixedConfig = tryFixChartConfig(aiResponse.getContent());
                if (fixedConfig == null) {
                    fixedConfig = getDefaultChartConfig(userGoal, csvData);
                }
                
                int costMs = (int) (System.currentTimeMillis() - startTime);
                modelRecordService.updateModelRecordStatus(modelRecordId, "partial", aiResponse.getContent(), "解析失败", costMs);
                
                Chart chart = saveChart(request.getName(), request.getGoal(), csvData, 
                        request.getChartType(), fixedConfig, "解析失败", user.getId());
                
                modelRecordService.updateChartId(modelRecordId, chart.getId());
                
                BiResponse biResponse = new BiResponse();
                biResponse.setGenChart(fixedConfig);
                biResponse.setGenResult("解析失败");
                biResponse.setChartId(chart.getId());
                return biResponse;
            }

            String genChart = parts[0];
            String genResult = parts[1];

            // 验证并修复图表配置
            if (!AiResponseParser.validateChartConfig(genChart)) {
                genChart = tryFixChartConfig(genChart);
                if (genChart == null) {
                    genChart = getDefaultChartConfig(userGoal, csvData);
                }
            }

            // 更新模型调用记录（成功）
            int costMs = (int) (System.currentTimeMillis() - startTime);
            modelRecordService.updateModelRecordStatus(modelRecordId, "success", aiResponse.getContent(), null, costMs,
                    aiResponse.getInputTokens(), aiResponse.getOutputTokens(), aiResponse.getTotalTokens());

            // 保存图表
            Chart chart = saveChart(request.getName(), request.getGoal(), csvData, 
                    request.getChartType(), genChart, genResult, user.getId());

            // 更新模型调用记录关联 chartId
            modelRecordService.updateChartId(modelRecordId, chart.getId());

            BiResponse biResponse = new BiResponse();
            biResponse.setGenChart(genChart);
            biResponse.setGenResult(genResult);
            biResponse.setChartId(chart.getId());
            return biResponse;

        } catch (Exception e) {
            // 更新模型调用记录（失败）
            if (modelRecordId != null) {
                int costMs = (int) (System.currentTimeMillis() - startTime);
                modelRecordService.updateModelRecordStatus(modelRecordId, "failed", null, e.getMessage(), costMs);
            }
            throw e;
        }
    }

    @Override
    public Long generateChartAsync(MultipartFile file, GenChartByAiRequest request, User user) {
        // 参数校验
        validateRequest(request, file);
        
        // 限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" + user.getId());

        // 获取 Prompt
        String promptQuery = getPrompt(request.getPromptId());

        // 解析 Excel 数据
        String csvData = ExcelUtils.excelToCsv(file);

        // 构建用户目标
        String userGoal = buildUserGoal(request.getGoal(), request.getChartType());

        // 构建完整请求内容
        String fullRequestContent = buildFullRequestContent(promptQuery, userGoal, csvData, request.getPromptId());

        // 保存图表（running 状态）
        Chart chart = saveChart(request.getName(), request.getGoal(), csvData, 
                request.getChartType(), null, null, user.getId());
        chart.setStatus("running");
        chartService.updateById(chart);

        // 插入任务日志
        Long taskLogId = createTaskLog(chart.getId());

        // 插入模型调用记录
        Long modelRecordId = createModelRecord(user.getId(), fullRequestContent);
        modelRecordService.updateChartId(modelRecordId, chart.getId());

        // 异步执行图表生成
        final Long finalChartId = chart.getId();
        final Long finalTaskLogId = taskLogId;
        final Long finalModelRecordId = modelRecordId;
        final String finalPromptQuery = promptQuery;

        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            Chart updateChart = new Chart();
            updateChart.setId(finalChartId);

            try {
                // 调用 AI 生成图表（带重试）
                AiResponse aiResponse = generateWithRetry(userGoal, csvData, finalPromptQuery);
                
                if (aiResponse == null) {
                    // 重试失败，使用默认配置
                    String defaultConfig = getDefaultChartConfig(userGoal, csvData);
                    String defaultAnalysis = "AI生成失败，使用默认图表配置。";
                    
                    updateChart.setGenChart(defaultConfig);
                    updateChart.setGenResult(defaultAnalysis);
                    updateChart.setStatus("partial");
                    
                    int costMs = (int) (System.currentTimeMillis() - startTime);
                    taskLogService.updateTaskLogStatus(finalTaskLogId, "partial", costMs, "AI生成失败");
                    modelRecordService.updateModelRecordStatus(finalModelRecordId, "partial", null, "AI生成失败", costMs);
                    log.warn("异步图表生成使用默认配置: chartId={}", finalChartId);
                    chartService.updateById(updateChart);
                    return;
                }

                // 解析 AI 响应
                String[] parts = AiResponseParser.parse(aiResponse.getContent());
                if (parts == null) {
                    String fixedConfig = tryFixChartConfig(aiResponse.getContent());
                    if (fixedConfig == null) {
                        fixedConfig = getDefaultChartConfig(userGoal, csvData);
                    }
                    
                    updateChart.setGenChart(fixedConfig);
                    updateChart.setGenResult("解析失败");
                    updateChart.setStatus("partial");
                    
                    int costMs = (int) (System.currentTimeMillis() - startTime);
                    taskLogService.updateTaskLogStatus(finalTaskLogId, "partial", costMs, "解析失败");
                    modelRecordService.updateModelRecordStatus(finalModelRecordId, "partial", aiResponse.getContent(), "解析失败", costMs);
                    chartService.updateById(updateChart);
                    return;
                }

                String genChart = parts[0];
                String genResult = parts[1];

                // 验证并修复图表配置
                if (!AiResponseParser.validateChartConfig(genChart)) {
                    genChart = tryFixChartConfig(genChart);
                    if (genChart == null) {
                        genChart = getDefaultChartConfig(userGoal, csvData);
                    }
                }

                // 更新图表
                updateChart.setGenChart(genChart);
                updateChart.setGenResult(genResult);
                updateChart.setStatus("succeed");

                // 更新任务日志和模型记录
                int costMs = (int) (System.currentTimeMillis() - startTime);
                taskLogService.updateTaskLogStatus(finalTaskLogId, "success", costMs, "执行成功");
                modelRecordService.updateModelRecordStatus(finalModelRecordId, "success", aiResponse.getContent(), null, costMs);

                log.info("异步图表生成成功: chartId={}", finalChartId);

            } catch (Exception e) {
                log.error("异步图表生成失败: chartId={}", finalChartId, e);
                handleAsyncError(finalChartId, finalTaskLogId, finalModelRecordId, 
                        startTime, e.getMessage(), null);
            } finally {
                chartService.updateById(updateChart);
            }
        }, threadPoolExecutor);

        return chart.getId();
    }

    @Override
    public Long generateChartAsyncMq(MultipartFile file, GenChartByAiRequest request, User user) {
        // 参数校验
        validateRequest(request, file);
        
        // 限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" + user.getId());

        // 获取 Prompt
        String promptQuery = getPrompt(request.getPromptId());

        // 解析 Excel 数据
        String csvData = ExcelUtils.excelToCsv(file);

        // 构建用户目标
        String userGoal = buildUserGoal(request.getGoal(), request.getChartType());

        // 构建完整请求内容
        String fullRequestContent = buildFullRequestContent(promptQuery, userGoal, csvData, request.getPromptId());

        // 保存图表（running 状态）
        Chart chart = saveChart(request.getName(), request.getGoal(), csvData, 
                request.getChartType(), null, null, user.getId());
        chart.setStatus("running");
        chartService.updateById(chart);

        // 插入任务日志
        createTaskLog(chart.getId());

        // 插入模型调用记录
        Long modelRecordId = createModelRecord(user.getId(), fullRequestContent);
        modelRecordService.updateChartId(modelRecordId, chart.getId());

        // 发送消息到 MQ
        biMessageProducer.sendMessage(chart.getId(), request.getPromptId());

        return chart.getId();
    }

    /**
     * MQ消息处理：执行图表生成（带重试机制）
     */
    @Override
    public ChartGenResult processChartGenTask(Long chartId, Long promptId) {
        // 1. 获取图表信息
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            log.error("图表不存在: chartId={}", chartId);
            return ChartGenResult.failed("图表不存在");
        }

        // 2. 获取 Prompt
        String promptQuery = null;
        if (promptId != null && promptId > 0) {
            Prompt prompt = promptService.getById(promptId);
            if (prompt != null) {
                promptQuery = prompt.getPromptQuery();
                promptService.incrementUsageCount(promptId);
            }
        }

        // 3. 带重试机制调用AI生成
        AiResponse aiResponse = generateWithRetry(chart.getGoal(), chart.getChartData(), promptQuery);

        if (aiResponse == null) {
            log.warn("AI生成重试{}次均失败，使用默认配置: chartId={}", MAX_RETRY_COUNT, chartId);
            String defaultChart = getDefaultChartConfig(chart.getGoal(), chart.getChartData());
            String analysisText = "AI生成失败（格式解析异常），系统已使用默认图表配置。请检查数据格式或重试。";
            return ChartGenResult.partial(defaultChart, analysisText, "AI生成失败，使用默认配置");
        }

        String result = aiResponse.getContent();

        // 4. 解析AI响应
        String[] parts = AiResponseParser.parse(result);
        if (parts == null || parts.length < 2 || StringUtils.isBlank(parts[0])) {
            log.warn("AI响应解析失败，尝试修复或使用默认配置: chartId={}", chartId);
            
            // 尝试修复配置
            String genChart = parts != null && StringUtils.isNotBlank(parts[0]) ? tryFixChartConfig(parts[0]) : null;
            
            if (genChart == null || !AiResponseParser.validateChartConfig(genChart)) {
                // 修复失败，使用默认配置
                genChart = getDefaultChartConfig(chart.getGoal(), chart.getChartData());
            }
            
            String genResult = parts != null && StringUtils.isNotBlank(parts[1]) ? parts[1] : "解析失败";
            return ChartGenResult.partial(genChart, genResult, "解析失败，使用修复/默认配置");
        }

        String genChart = parts[0];
        String genResult = parts[1];

        // 5. 验证并修复图表配置
        if (!AiResponseParser.validateChartConfig(genChart)) {
            log.warn("图表配置验证失败，尝试修复: chartId={}", chartId);
            genChart = tryFixChartConfig(genChart);
            
            if (!AiResponseParser.validateChartConfig(genChart)) {
                log.warn("配置修复失败，使用默认配置: chartId={}", chartId);
                genChart = getDefaultChartConfig(chart.getGoal(), chart.getChartData());
            }
        }

        // 6. 返回成功结果
        return ChartGenResult.success(genChart, genResult, aiResponse);
    }

    /**
     * 带重试机制的AI调用（公开方法，供外部调用）
     * 当解析失败时自动重试，最多重试MAX_RETRY_COUNT次
     */
    @Override
    public AiResponse generateWithRetry(String goal, String csvData, String promptQuery) {
        int retryCount = 0;
        long intervalMs = RETRY_INTERVAL_MS;
        
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                AiResponse response = aiManager.doChartChatWithInfo(goal, csvData, promptQuery);
                
                if (response != null && StringUtils.isNotBlank(response.getContent())) {
                    String[] parts = AiResponseParser.parse(response.getContent());
                    
                    if (parts != null && parts.length >= 2 && 
                        StringUtils.isNotBlank(parts[0]) && 
                        AiResponseParser.validateChartConfig(parts[0])) {
                        // 验证通过，直接返回
                        log.info("AI生成成功: 第{}次尝试", retryCount + 1);
                        return response;
                    }
                    
                    log.warn("第{}次生成格式验证失败，尝试重试: goal长度={}", retryCount + 1, goal.length());
                }
                
            } catch (Exception e) {
                log.warn("第{}次生成异常: {}, goal长度={}", retryCount + 1, e.getMessage(), goal.length());
            }
            
            retryCount++;
            
            if (retryCount < MAX_RETRY_COUNT) {
                try {
                    log.info("等待{}ms后进行第{}次重试", intervalMs, retryCount + 1);
                    Thread.sleep(intervalMs);
                    intervalMs *= 2; // 指数退避
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.warn("AI生成重试{}次均失败", MAX_RETRY_COUNT);
        return null;
    }

    /**
     * 获取默认图表配置（当AI生成失败时使用）
     */
    @Override
    public String getDefaultChartConfig(String goal, String csvData) {
        int rowCount = countRows(csvData);
        String chartType = rowCount > 10 ? "bar" : "pie";
        
        return String.format(
            "{\"title\":{\"text\":\"%s\"},\"xAxis\":{\"type\":\"category\",\"data\":[\"数据1\",\"数据2\",\"数据3\",\"数据4\",\"数据5\"]}," +
            "\"yAxis\":{\"type\":\"value\"},\"series\":[{\"type\":\"%s\",\"name\":\"数据\",\"data\":[10,20,30,40,50]}]}",
            StringUtils.isNotBlank(goal) ? goal : "数据分析图表", chartType
        );
    }

    /**
     * 尝试修复图表配置JSON
     */
    @Override
    public String tryFixChartConfig(String config) {
        if (StringUtils.isBlank(config)) {
            return null;
        }
        
        try {
            // 移除可能的转义字符和代码块标记
            config = config.replace("\\\"", "\"");
            config = config.replaceAll("^```(json|javascript|js)?\\s*", "");
            config = config.replaceAll("\\s*```$", "");
            
            // 解析JSON
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(config);
            
            // 检查并修复必要字段
            if (!json.containsKey("title")) {
                json.put("title", new com.alibaba.fastjson.JSONObject());
            }
            if (!json.containsKey("xAxis")) {
                json.put("xAxis", new com.alibaba.fastjson.JSONObject().fluentPut("type", "category"));
            }
            if (!json.containsKey("yAxis")) {
                json.put("yAxis", new com.alibaba.fastjson.JSONObject().fluentPut("type", "value"));
            }
            if (!json.containsKey("series")) {
                json.put("series", new java.util.ArrayList<>());
            }
            
            return json.toJSONString();
            
        } catch (Exception e) {
            log.warn("配置修复失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验请求参数
     */
    private void validateRequest(GenChartByAiRequest request, MultipartFile file) {
        String goal = request.getGoal();
        String name = request.getName();
        
        ThrowUtils.throwIf(StringUtils.isBlank(goal), 
                com.chartflow.core.common.ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, 
                com.chartflow.core.common.ErrorCode.PARAMS_ERROR, "名称过长");

        // 文件校验
        ThrowUtils.throwIf(file == null || file.isEmpty(), 
                com.chartflow.core.common.ErrorCode.PARAMS_ERROR, "文件为空");
        
        long size = file.getSize();
        ThrowUtils.throwIf(size > MAX_FILE_SIZE, 
                com.chartflow.core.common.ErrorCode.PARAMS_ERROR, "文件超过 50M");

        String originalFilename = file.getOriginalFilename();
        ThrowUtils.throwIf(StringUtils.isBlank(originalFilename), 
                com.chartflow.core.common.ErrorCode.PARAMS_ERROR, "文件名不能为空");

        String suffix = getFileSuffix(originalFilename);
        ThrowUtils.throwIf(!VALID_FILE_SUFFIX_LIST.contains(suffix), 
                com.chartflow.core.common.ErrorCode.PARAMS_ERROR, "文件后缀非法");
    }

    /**
     * 获取文件后缀
     */
    private String getFileSuffix(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 获取 Prompt（优先使用自定义，否则使用默认）
     */
    private String getPrompt(Long promptId) {
        if (promptId != null && promptId > 0) {
            Prompt prompt = promptService.getById(promptId);
            if (prompt != null) {
                promptService.incrementUsageCount(promptId);
                return prompt.getPromptQuery();
            }
        }
        return chartConfig.getDefaultPrompt();
    }

    /**
     * 构建用户目标（包含图表类型）
     */
    private String buildUserGoal(String goal, String chartType) {
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        return userGoal;
    }

    /**
     * 构建完整的请求内容
     */
    private String buildFullRequestContent(String promptQuery, String userGoal, String csvData, Long promptId) {
        if (promptId != null && promptId > 0) {
            // 使用自定义 prompt
            return promptQuery + "\n====================\n" +
                    "分析需求：\n" + userGoal + "\n" +
                    "原始数据：\n" + csvData;
        } else {
            // 使用默认 prompt
            return promptQuery + "\n" +
                    "分析需求：\n" + userGoal + "\n" +
                    "原始数据：\n" + csvData;
        }
    }

    /**
     * 保存图表到数据库
     */
    private Chart saveChart(String name, String goal, String chartData, String chartType,
                            String genChart, String genResult, Long userId) {
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(chartData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(userId);
        chart.setStatus("succeed");
        
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, 
                com.chartflow.core.common.ErrorCode.SYSTEM_ERROR, "图表保存失败");
        
        return chart;
    }

    /**
     * 创建任务日志
     */
    private Long createTaskLog(Long chartId) {
        TaskLogAddRequest taskLogAddRequest = new TaskLogAddRequest();
        taskLogAddRequest.setChartId(String.valueOf(chartId));
        taskLogAddRequest.setStatus("running");
        return taskLogService.addTaskLog(taskLogAddRequest);
    }

    /**
     * 创建模型调用记录
     */
    private Long createModelRecord(Long userId, String requestContent) {
        ModelRecordAddRequest modelRecordAddRequest = new ModelRecordAddRequest();
        modelRecordAddRequest.setUserId(userId);
        modelRecordAddRequest.setModelName("qwen2.5:7b");
        modelRecordAddRequest.setInvocationType("chartGen");
        modelRecordAddRequest.setRequestContent(requestContent);
        modelRecordAddRequest.setStatus("running");
        return modelRecordService.addModelRecord(modelRecordAddRequest);
    }

    /**
     * 处理异步任务错误
     */
    private void handleAsyncError(Long chartId, Long taskLogId, Long modelRecordId, 
                                  long startTime, String errorMessage, String result) {
        // 更新图表状态
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(errorMessage);
        chartService.updateById(updateChart);

        // 更新任务日志
        int costMs = (int) (System.currentTimeMillis() - startTime);
        taskLogService.updateTaskLogStatus(taskLogId, "failed", costMs, errorMessage);

        // 更新模型调用记录
        modelRecordService.updateModelRecordStatus(modelRecordId, "failed", result, errorMessage, costMs);

        log.error("图表生成失败: chartId={}, error={}", chartId, errorMessage);
    }

    /**
     * 统计CSV行数
     */
    private int countRows(String csvData) {
        if (StringUtils.isBlank(csvData)) return 0;
        return csvData.split("\n").length;
    }
}