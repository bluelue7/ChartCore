package com.chartflow.core.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chartflow.core.annotation.AuthCheck;
import com.chartflow.core.common.BaseResponse;
import com.chartflow.core.common.DeleteRequest;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.common.ResultUtils;
import com.chartflow.core.constant.UserConstant;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.exception.ThrowUtils;
import com.chartflow.core.model.dto.chart.ChartAddRequest;
import com.chartflow.core.model.dto.chart.ChartQueryRequest;
import com.chartflow.core.model.dto.chart.ChartUpdateRequest;
import com.chartflow.core.model.dto.chart.GenChartByAiRequest;
import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.model.entity.User;
import com.chartflow.core.model.vo.BiResponse;
import com.chartflow.core.service.ChartGenService;
import com.chartflow.core.service.ChartService;
import com.chartflow.core.service.EmailService;
import com.chartflow.core.service.UserService;
import com.chartflow.core.utils.EmailContentBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 图表接口
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
    private ChartGenService chartGenService;

    @Resource
    private EmailService emailService;

    /**
     * 创建图表
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
     * 删除图表
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
     * 更新图表（管理员）
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


    /**
     * 智能分析（同步）
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        log.info("开始同步生成图表");
        User loginUser = userService.getLoginUser(request);
        BiResponse response = chartGenService.generateChartSync(multipartFile, genChartByAiRequest, loginUser);
        log.info("同步图表生成完成: chartId={}", response.getChartId());
        return ResultUtils.success(response);
    }

    /**
     * 智能分析（异步线程池）
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        log.info("开始异步生成图表（线程池）");
        User loginUser = userService.getLoginUser(request);
        Long chartId = chartGenService.generateChartAsync(multipartFile, genChartByAiRequest, loginUser);
        
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        log.info("异步图表生成任务已提交: chartId={}", chartId);
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步消息队列）
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                       GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        log.info("开始异步生成图表（MQ）");
        User loginUser = userService.getLoginUser(request);
        Long chartId = chartGenService.generateChartAsyncMq(multipartFile, genChartByAiRequest, loginUser);
        
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        log.info("MQ消息已发送: chartId={}", chartId);
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
        
        // 异步发送邮件通知（使用配置的重试参数）
        final String userEmail = loginUser.getEmail();
        final String chartName = genChartByAiRequest.getName();
        
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
                    String subject = "图表生成完成 - " + (StringUtils.isNotBlank(chartName) ? chartName : "未命名图表");
                    String htmlContent = EmailContentBuilder.buildChartNotificationEmail(chart);
                    emailService.sendHtmlEmail(userEmail, subject, htmlContent);
                    log.info("邮件通知发送成功: chartId={}, email={}", chartId, userEmail);
                } else {
                    log.warn("邮件通知失败: 图表生成未完成或失败, chartId={}", chartId);
                }
            } catch (Exception e) {
                log.error("发送邮件通知失败: chartId={}", chartId, e);
            }
        }).start();
        
        return response;
    }
}