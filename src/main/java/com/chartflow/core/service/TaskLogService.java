package com.chartflow.core.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chartflow.core.model.dto.tasklog.TaskLogAddRequest;
import com.chartflow.core.model.dto.tasklog.TaskLogQueryRequest;
import com.chartflow.core.model.entity.TaskLog;

/**
 * 任务执行日志服务
 */
public interface TaskLogService extends IService<TaskLog> {

    /**
     * 获取查询条件封装
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<TaskLog> getQueryWrapper(TaskLogQueryRequest queryRequest);

    /**
     * 添加任务日志
     *
     * @param addRequest 添加请求
     * @return 日志ID
     */
    Long addTaskLog(TaskLogAddRequest addRequest);

    /**
     * 更新任务日志状态
     *
     * @param id 日志ID
     * @param status 状态
     * @param costMs 耗时
     * @param execMessage 执行信息
     * @return 是否成功
     */
    boolean updateTaskLogStatus(Long id, String status, Integer costMs, String execMessage);
}
