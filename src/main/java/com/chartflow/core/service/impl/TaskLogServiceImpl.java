package com.chartflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chartflow.core.mapper.TaskLogMapper;
import com.chartflow.core.model.dto.tasklog.TaskLogAddRequest;
import com.chartflow.core.model.dto.tasklog.TaskLogQueryRequest;
import com.chartflow.core.model.entity.TaskLog;
import com.chartflow.core.service.TaskLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 任务执行日志服务实现
 */
@Service
@Slf4j
public class TaskLogServiceImpl extends ServiceImpl<TaskLogMapper, TaskLog> implements TaskLogService {

    @Override
    public QueryWrapper<TaskLog> getQueryWrapper(TaskLogQueryRequest queryRequest) {
        QueryWrapper<TaskLog> queryWrapper = new QueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        Long id = queryRequest.getId();
        String chartId = queryRequest.getChartId();
        String status = queryRequest.getStatus();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(chartId), "chartId", chartId);
        queryWrapper.eq(StringUtils.isNotBlank(status), "status", status);
        queryWrapper.orderByDesc("createTime");
        return queryWrapper;
    }

    @Override
    public Long addTaskLog(TaskLogAddRequest addRequest) {
        TaskLog taskLog = new TaskLog();
        BeanUtils.copyProperties(addRequest, taskLog);
        taskLog.setCreateTime(new Date());
        taskLog.setUpdateTime(new Date());
        boolean save = this.save(taskLog);
        if (!save) {
            log.error("添加任务日志失败");
            return null;
        }
        return taskLog.getId();
    }

    @Override
    public boolean updateTaskLogStatus(Long id, String status, Integer costMs, String execMessage) {
        TaskLog taskLog = new TaskLog();
        taskLog.setId(id);
        taskLog.setStatus(status);
        taskLog.setCostMs(costMs);
        taskLog.setExecMessage(execMessage);
        taskLog.setUpdateTime(new Date());
        return this.updateById(taskLog);
    }
}
