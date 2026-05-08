package com.chartflow.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chartflow.core.common.BaseResponse;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.common.ResultUtils;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.model.dto.tasklog.TaskLogAddRequest;
import com.chartflow.core.model.dto.tasklog.TaskLogQueryRequest;
import com.chartflow.core.model.entity.TaskLog;
import com.chartflow.core.service.TaskLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 任务执行日志接口
 */
@RestController
@RequestMapping("/taskLog")
@Slf4j
public class TaskLogController {

    @Resource
    private TaskLogService taskLogService;

    /**
     * 添加任务日志
     *
     * @param addRequest 添加请求
     * @return 日志ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTaskLog(@RequestBody TaskLogAddRequest addRequest) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long logId = taskLogService.addTaskLog(addRequest);
        if (logId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加任务日志失败");
        }
        return ResultUtils.success(logId);
    }

    /**
     * 根据ID获取任务日志
     *
     * @param id 日志ID
     * @return 任务日志
     */
    @GetMapping("/get")
    public BaseResponse<TaskLog> getTaskLogById(@RequestParam Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        TaskLog taskLog = taskLogService.getById(id);
        if (taskLog == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(taskLog);
    }

    /**
     * 分页获取任务日志列表
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<TaskLog>> listTaskLogByPage(@RequestBody TaskLogQueryRequest queryRequest) {
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<TaskLog> page = taskLogService.page(new Page<>(current, size),
                taskLogService.getQueryWrapper(queryRequest));
        return ResultUtils.success(page);
    }

    /**
     * 根据chartId获取任务日志
     *
     * @param chartId 图表ID
     * @return 任务日志
     */
    @GetMapping("/get/byChartId")
    public BaseResponse<TaskLog> getTaskLogByChartId(@RequestParam String chartId) {
        if (chartId == null || chartId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        TaskLogQueryRequest queryRequest = new TaskLogQueryRequest();
        queryRequest.setChartId(chartId);
        TaskLog taskLog = taskLogService.getOne(taskLogService.getQueryWrapper(queryRequest));
        return ResultUtils.success(taskLog);
    }
}
