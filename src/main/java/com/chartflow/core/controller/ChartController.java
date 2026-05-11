package com.chartflow.core.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chartflow.core.annotation.AuthCheck;
import com.chartflow.core.bizmq.BiMessageProducer;
import com.chartflow.core.common.BaseResponse;
import com.chartflow.core.common.DeleteRequest;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.common.ResultUtils;
import com.chartflow.core.constant.UserConstant;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.exception.ThrowUtils;
import com.chartflow.core.manager.AiManager;
import com.chartflow.core.manager.RedisLimiterManager;
import com.chartflow.core.model.dto.chart.*;
import com.chartflow.core.model.dto.modelrecord.ModelRecordAddRequest;
import com.chartflow.core.model.dto.tasklog.TaskLogAddRequest;
import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.model.entity.Prompt;
import com.chartflow.core.model.entity.User;
import com.chartflow.core.model.vo.BiResponse;
import com.chartflow.core.service.*;
import com.chartflow.core.utils.EmailContentBuilder;
import com.chartflow.core.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 *
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private LocalAiService localAiService;

    @Resource
    private PromptService promptService;

    @Resource
    private ModelRecordService modelRecordService;

    @Resource
    private TaskLogService taskLogService;

    @Resource
    private EmailService emailService;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param chartUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest,
            HttpServletRequest request) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        boolean result = chartService.updateById(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取单个图表详情
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/{id}")
    public BaseResponse<Chart> getChartById(@PathVariable long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 检查权限：只能查看自己的图表或管理员可以查看所有
        User loginUser = userService.getLoginUser(request);
        if (!chart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 获取当前用户的图表列表（分页）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/my")
    public BaseResponse<Page<Chart>> listMyChartByPage(ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        QueryWrapper<Chart> queryWrapper = chartService.getQueryWrapper(chartQueryRequest);
        // 只查询当前用户的图表
        queryWrapper.eq("userId", loginUser.getId());
        Page<Chart> chartPage = chartService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(chartPage);
    }

    /**
     * 获取所有图表列表（管理员）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @GetMapping("")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listAllChartByPage(ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        QueryWrapper<Chart> queryWrapper = chartService.getQueryWrapper(chartQueryRequest);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(chartPage);
    }


    // endregion

    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long promptId = genChartByAiRequest.getPromptId();
        log.info("项目测试",name);
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小（支持更大的文件上传）
        final long MAX_FILE_SIZE = 50 * 1024 * 1024L; // 50MB
        ThrowUtils.throwIf(size > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件超过 50M");
        // 校验文件后缀 aaa.xlsx
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 获取Prompt
        String promptQuery = null;
        if (promptId != null && promptId > 0) {
            Prompt prompt = promptService.getById(promptId);
            if (prompt != null) {
                promptQuery = prompt.getPromptQuery();
                // 增加使用次数
                promptService.incrementUsageCount(promptId);
            }
        }

        // 如果没有选择prompt，使用默认prompt
        if (StringUtils.isBlank(promptQuery)) {
            promptQuery = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                    "分析需求：\n" +
                    "{数据分析的需求或者目标}\n" +
                    "原始数据：\n" +
                    "{csv格式的原始数据，用,作为分隔符}\n" +
                    "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                    "【【【【【\n" +
                    "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释，直接以{\"title\": {开头}\n" +
                    "【【【【【\n" +
                    "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
        }

        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }

        // 调用AI前记录
        long startTime = System.currentTimeMillis();
        Long modelRecordId = null;
        String result = null;
        try {
            // 构建完整的请求内容（传给AI的所有内容）
            String fullRequestContent;
            if (promptId != null && promptId > 0) {
                // 使用自定义prompt
                fullRequestContent = promptQuery + "\n====================\n" +
                        "分析需求：\n" + userGoal + "\n" +
                        "原始数据：\n" + csvData;
            } else {
                // 使用默认prompt
                fullRequestContent = promptQuery + "\n" +
                        "分析需求：\n" + userGoal + "\n" +
                        "原始数据：\n" + csvData;
            }

            // 插入模型调用记录（running状态）
            com.chartflow.core.model.dto.modelrecord.ModelRecordAddRequest modelRecordAddRequest = 
                new com.chartflow.core.model.dto.modelrecord.ModelRecordAddRequest();
            modelRecordAddRequest.setUserId(loginUser.getId());
            modelRecordAddRequest.setModelName("qwen2.5:7b"); // 可以从配置读取
            modelRecordAddRequest.setInvocationType("chartGen");
            modelRecordAddRequest.setRequestContent(fullRequestContent);
            modelRecordAddRequest.setStatus("running");
            modelRecordId = modelRecordService.addModelRecord(modelRecordAddRequest);

            result = aiManager.doChartChat(userGoal, csvData, promptQuery);
            log.info("项目测试result:{} ",result);

            // 更新模型调用记录（success状态）
            int costMs = (int)(System.currentTimeMillis() - startTime);
            modelRecordService.updateModelRecordStatus(modelRecordId, "success", result, null, costMs);

        } catch (Exception e) {
            // 更新模型调用记录（failed状态）
            if (modelRecordId != null) {
                int costMs = (int)(System.currentTimeMillis() - startTime);
                modelRecordService.updateModelRecordStatus(modelRecordId, "failed", null, e.getMessage(), costMs);
            }
            throw e;
        }

        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setStatus("succeed");
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 更新模型调用记录，关联chartId
        if (modelRecordId != null) {
            com.chartflow.core.model.entity.ModelRecord modelRecord = new com.chartflow.core.model.entity.ModelRecord();
            modelRecord.setId(modelRecordId);
            modelRecord.setChartId(chart.getId());
            modelRecordService.updateById(modelRecord);
        }

        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long promptId = genChartByAiRequest.getPromptId();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小（支持更大的文件上传）
        final long MAX_FILE_SIZE = 50 * 1024 * 1024L; // 50MB
        ThrowUtils.throwIf(size > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件超过 50M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 获取Prompt
        String promptQuery = null;
        if (promptId != null && promptId > 0) {
            Prompt prompt = promptService.getById(promptId);
            if (prompt != null) {
                promptQuery = prompt.getPromptQuery();
                // 增加使用次数
                promptService.incrementUsageCount(promptId);
            }
        }

        // 如果没有选择prompt，使用默认prompt
        if (StringUtils.isBlank(promptQuery)) {
            promptQuery = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                    "分析需求：\n" +
                    "{数据分析的需求或者目标}\n" +
                    "原始数据：\n" +
                    "{csv格式的原始数据，用,作为分隔符}\n" +
                    "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                    "【【【【【\n" +
                    "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释，以 {\"title\": { 开头 " +
                    "【【【【【\n" +
                    "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
        }

        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }

        // 构建完整的请求内容（传给AI的所有内容）
        String fullRequestContent;
        if (promptId != null && promptId > 0) {
            // 使用自定义prompt
            fullRequestContent = promptQuery + "\n====================\n" +
                    "分析需求：\n" + userGoal + "\n" +
                    "原始数据：\n" + csvData;
        } else {
            // 使用默认prompt
            fullRequestContent = promptQuery + "\n" +
                    "分析需求：\n" + userGoal + "\n" +
                    "原始数据：\n" + csvData;
        }

        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setStatus("running");
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 插入任务日志（running状态）
        TaskLogAddRequest taskLogAddRequest = new TaskLogAddRequest();
        taskLogAddRequest.setChartId(String.valueOf(chart.getId()));
        taskLogAddRequest.setStatus("running");
        Long taskLogId = taskLogService.addTaskLog(taskLogAddRequest);

        // 插入模型调用记录（running状态）
        ModelRecordAddRequest modelRecordAddRequest = new ModelRecordAddRequest();
        modelRecordAddRequest.setChartId(chart.getId());
        modelRecordAddRequest.setUserId(loginUser.getId());
        modelRecordAddRequest.setModelName("qwen2.5:7b");
        modelRecordAddRequest.setInvocationType("chartGen");
        modelRecordAddRequest.setRequestContent(fullRequestContent);
        modelRecordAddRequest.setStatus("running");
        Long modelRecordId = modelRecordService.addModelRecord(modelRecordAddRequest);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        final Long finalTaskLogId = taskLogId;
        final Long finalModelRecordId = modelRecordId;
        final Long chartId = chart.getId();
        final String finalPromptQuery = promptQuery;

        // 执行异步任务
        String finalUserGoal = userGoal;
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartId);
            try {
                String result = aiManager.doChartChat(finalUserGoal, csvData, finalPromptQuery);
                String[] splits = result.split("【【【【【");
                if (splits.length < 3) {
                    chart.setStatus("failed");
                    chartService.updateById(chart);
                    // 更新任务日志（failed状态）
                    taskLogService.updateTaskLogStatus(finalTaskLogId, "failed", 
                        (int)(System.currentTimeMillis() - startTime), "AI生成格式错误");
                    // 更新模型调用记录（failed状态）
                    modelRecordService.updateModelRecordStatus(finalModelRecordId, "failed", null, 
                        "AI生成格式错误", (int)(System.currentTimeMillis() - startTime));
                    log.error("AI 生成错误");
                    return;
                }
                String genChart = splits[1].trim();
                String genResult = splits[2].trim();
                updateChartResult.setGenChart(genChart);
                updateChartResult.setGenResult(genResult);
                updateChartResult.setStatus("succeed");

                int costMs = (int)(System.currentTimeMillis() - startTime);
                // 更新任务日志（success状态）
                taskLogService.updateTaskLogStatus(finalTaskLogId, "success", costMs, "执行成功");
                // 更新模型调用记录（success状态）
                modelRecordService.updateModelRecordStatus(finalModelRecordId, "success", result, null, costMs);

            } catch (Exception e) {
                chart.setStatus("failed");
                int costMs = (int)(System.currentTimeMillis() - startTime);
                // 更新任务日志（failed状态）
                taskLogService.updateTaskLogStatus(finalTaskLogId, "failed", costMs, e.getMessage());
                // 更新模型调用记录（failed状态）
                modelRecordService.updateModelRecordStatus(finalModelRecordId, "failed", null, 
                    e.getMessage(), costMs);
                log.error("gen_chart_error", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
            } finally {
                chartService.updateById(updateChartResult);
            }
        }, threadPoolExecutor);

        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long promptId = genChartByAiRequest.getPromptId();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小（支持更大的文件上传）
        final long MAX_FILE_SIZE = 50 * 1024 * 1024L; // 50MB
        ThrowUtils.throwIf(size > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件超过 50M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 获取Prompt
        String promptQuery = null;
        if (promptId != null && promptId > 0) {
            Prompt prompt = promptService.getById(promptId);
            if (prompt != null) {
                promptQuery = prompt.getPromptQuery();
                // 增加使用次数
                promptService.incrementUsageCount(promptId);
            }
        }

        // 如果没有选择prompt，使用默认prompt
        if (StringUtils.isBlank(promptQuery)) {
            promptQuery = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                    "分析需求：\n" +
                    "{数据分析的需求或者目标}\n" +
                    "原始数据：\n" +
                    "{csv格式的原始数据，用,作为分隔符}\n" +
                    "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                    "【【【【【\n" +
                    "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释，直接以{\n\"title\": {开头" +
                    "【【【【【\n" +
                    "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
        }

        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }

        // 构建完整的请求内容（传给AI的所有内容）
        String fullRequestContent;
        if (promptId != null && promptId > 0) {
            // 使用自定义prompt
            fullRequestContent = promptQuery + "\n====================\n" +
                    "分析需求：\n" + userGoal + "\n" +
                    "原始数据：\n" + csvData;
        } else {
            // 使用默认prompt
            fullRequestContent = promptQuery + "\n" +
                    "分析需求：\n" + userGoal + "\n" +
                    "原始数据：\n" + csvData;
        }

        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setStatus("running");
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 插入任务日志（running状态）
        com.chartflow.core.model.dto.tasklog.TaskLogAddRequest taskLogAddRequest = 
            new com.chartflow.core.model.dto.tasklog.TaskLogAddRequest();
        taskLogAddRequest.setChartId(String.valueOf(chart.getId()));
        taskLogAddRequest.setStatus("running");
        Long taskLogId = taskLogService.addTaskLog(taskLogAddRequest);

        // 插入模型调用记录（running状态）
        com.chartflow.core.model.dto.modelrecord.ModelRecordAddRequest modelRecordAddRequest = 
            new com.chartflow.core.model.dto.modelrecord.ModelRecordAddRequest();
        modelRecordAddRequest.setChartId(chart.getId());
        modelRecordAddRequest.setUserId(loginUser.getId());
        modelRecordAddRequest.setModelName("qwen2.5:7b");
        modelRecordAddRequest.setInvocationType("chartGen");
        modelRecordAddRequest.setRequestContent(fullRequestContent);
        modelRecordAddRequest.setStatus("running");
        Long modelRecordId = modelRecordService.addModelRecord(modelRecordAddRequest);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        long newChartId = chart.getId();
        biMessageProducer.sendMessage(newChartId, promptId);

        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步）并发送邮件通知
     */
    @PostMapping("/gen/async/notify")
    public BaseResponse<BiResponse> genChartByAiAsyncWithNotify(
            @RequestPart("file") MultipartFile multipartFile,
            GenChartByAiRequest genChartByAiRequest,
            HttpServletRequest request) {
        
        User loginUser = userService.getLoginUser(request);
        
        // 检查用户是否绑定邮箱
        if (StringUtils.isBlank(loginUser.getEmail())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户未绑定邮箱，请先绑定邮箱");
        }
        
        // 检查邮件服务是否可用
        if (!emailService.isAvailable()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件服务暂不可用");
        }
        
        // 调用异步生成接口
        BaseResponse<BiResponse> response = genChartByAiAsync(multipartFile, genChartByAiRequest, request);
        long chartId = response.getData().getChartId();
        
        // 异步发送邮件通知
        final String userEmail = loginUser.getEmail();
        new Thread(() -> {
            try {
                int maxRetries = 30;
                Chart chart = null;
                for (int i = 0; i < maxRetries; i++) {
                    chart = chartService.getById(chartId);
                    if (chart != null && "succeed".equals(chart.getStatus())) {
                        break;
                    }
                    Thread.sleep(2000);
                }
                
                if (chart != null && "succeed".equals(chart.getStatus())) {
                    String subject = "图表生成完成 - " + (StringUtils.isNotBlank(chart.getName()) ? chart.getName() : "未命名图表");
                    String htmlContent = EmailContentBuilder.buildChartNotificationEmail(chart);
                    emailService.sendHtmlEmail(userEmail, subject, htmlContent);
                }
            } catch (Exception e) {
                log.error("发送邮件通知失败", e);
            }
        }).start();
        
        return response;
    }
}
