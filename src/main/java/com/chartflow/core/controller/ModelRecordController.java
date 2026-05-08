package com.chartflow.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chartflow.core.common.BaseResponse;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.common.ResultUtils;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.model.dto.modelrecord.ModelRecordAddRequest;
import com.chartflow.core.model.dto.modelrecord.ModelRecordQueryRequest;
import com.chartflow.core.model.entity.ModelRecord;
import com.chartflow.core.service.ModelRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 大模型调用记录接口
 */
@RestController
@RequestMapping("/modelRecord")
@Slf4j
public class ModelRecordController {

    @Resource
    private ModelRecordService modelRecordService;

    /**
     * 添加模型调用记录
     *
     * @param addRequest 添加请求
     * @return 记录ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addModelRecord(@RequestBody ModelRecordAddRequest addRequest) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long recordId = modelRecordService.addModelRecord(addRequest);
        if (recordId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加模型调用记录失败");
        }
        return ResultUtils.success(recordId);
    }

    /**
     * 根据ID获取模型调用记录
     *
     * @param id 记录ID
     * @return 模型调用记录
     */
    @GetMapping("/get")
    public BaseResponse<ModelRecord> getModelRecordById(@RequestParam Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ModelRecord modelRecord = modelRecordService.getById(id);
        if (modelRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(modelRecord);
    }

    /**
     * 分页获取模型调用记录列表
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ModelRecord>> listModelRecordByPage(@RequestBody ModelRecordQueryRequest queryRequest) {
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<ModelRecord> page = modelRecordService.page(new Page<>(current, size),
                modelRecordService.getQueryWrapper(queryRequest));
        return ResultUtils.success(page);
    }

    /**
     * 根据chartId获取模型调用记录列表
     *
     * @param chartId 图表ID
     * @param current 当前页
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/list/byChartId")
    public BaseResponse<Page<ModelRecord>> listModelRecordByChartId(
            @RequestParam Long chartId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        if (chartId == null || chartId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ModelRecordQueryRequest queryRequest = new ModelRecordQueryRequest();
        queryRequest.setChartId(chartId);
        queryRequest.setCurrent((int) current);
        queryRequest.setPageSize((int) size);
        Page<ModelRecord> page = modelRecordService.page(new Page<>(current, size),
                modelRecordService.getQueryWrapper(queryRequest));
        return ResultUtils.success(page);
    }
}
