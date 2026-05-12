package com.chartflow.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chartflow.core.common.BaseResponse;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.common.ResultUtils;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.model.dto.feedback.ChartFeedbackAddRequest;
import com.chartflow.core.model.dto.feedback.ChartFeedbackQueryRequest;
import com.chartflow.core.model.dto.feedback.ChartFeedbackUpdateRequest;
import com.chartflow.core.model.entity.ChartFeedback;
import com.chartflow.core.model.entity.User;
import com.chartflow.core.service.ChartFeedbackService;
import com.chartflow.core.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图表反馈接口
 *
 */
@RestController
@RequestMapping("/chart-feedback")
@Slf4j
public class ChartFeedbackController {

    @Resource
    private ChartFeedbackService chartFeedbackService;

    @Resource
    private UserService userService;

    /**
     * 添加图表反馈
     *
     * @param request 反馈添加请求
     * @param httpRequest 请求对象
     * @return 反馈ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addFeedback(@RequestBody ChartFeedbackAddRequest request,
            HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        
        // 校验评分
        Integer rating = request.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分必须在1-5之间");
        }

        ChartFeedback feedback = new ChartFeedback();
        BeanUtils.copyProperties(request, feedback);
        feedback.setUserId(loginUser.getId());
        
        boolean result = chartFeedbackService.save(feedback);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存反馈失败");
        }
        return ResultUtils.success(feedback.getId());
    }

    /**
     * 删除图表反馈
     *
     * @param id 反馈ID
     * @param httpRequest 请求对象
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteFeedback(@RequestParam long id, HttpServletRequest httpRequest) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        
        ChartFeedback feedback = chartFeedbackService.getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "反馈不存在");
        }
        
        // 只有管理员或反馈所有者可以删除
        if (!loginUser.getId().equals(feedback.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除");
        }
        
        boolean result = chartFeedbackService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新图表反馈
     *
     * @param request 反馈更新请求
     * @param httpRequest 请求对象
     * @return 更新结果
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updateFeedback(@RequestBody ChartFeedbackUpdateRequest request,
            HttpServletRequest httpRequest) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        User loginUser = userService.getLoginUser(httpRequest);
        Long id = request.getId();
        
        ChartFeedback feedback = chartFeedbackService.getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "反馈不存在");
        }
        
        // 只有管理员或反馈所有者可以更新
        if (!loginUser.getId().equals(feedback.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限更新");
        }
        
        // 校验评分
        Integer rating = request.getRating();
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分必须在1-5之间");
        }

        ChartFeedback updateFeedback = new ChartFeedback();
        BeanUtils.copyProperties(request, updateFeedback);
        
        boolean result = chartFeedbackService.updateById(updateFeedback);
        return ResultUtils.success(result);
    }

    /**
     * 根据ID获取反馈详情
     *
     * @param id 反馈ID
     * @return 反馈详情
     */
    @GetMapping("/get")
    public BaseResponse<ChartFeedback> getFeedbackById(@RequestParam long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ChartFeedback feedback = chartFeedbackService.getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "反馈不存在");
        }
        return ResultUtils.success(feedback);
    }

    /**
     * 分页获取反馈列表
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ChartFeedback>> listFeedbackByPage(@RequestBody ChartFeedbackQueryRequest queryRequest) {
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<ChartFeedback> page = chartFeedbackService.page(new Page<>(current, size),
                chartFeedbackService.getQueryWrapper(queryRequest));
        return ResultUtils.success(page);
    }

    /**
     * 获取当前用户的反馈列表
     *
     * @param page 当前页码
     * @param size 每页大小
     * @param httpRequest 请求对象
     * @return 分页结果
     */
    @GetMapping("/list/my")
    public BaseResponse<Page<ChartFeedback>> listMyFeedback(@RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        ChartFeedbackQueryRequest queryRequest = new ChartFeedbackQueryRequest();
        queryRequest.setUserId(loginUser.getId());
        queryRequest.setCurrent((int) page);
        queryRequest.setPageSize((int) size);
        
        Page<ChartFeedback> feedbackPage = chartFeedbackService.page(new Page<>(page, size),
                chartFeedbackService.getQueryWrapper(queryRequest));
        return ResultUtils.success(feedbackPage);
    }

    /**
     * 获取指定图表的反馈列表
     *
     * @param chartId 图表ID
     * @param page 当前页码
     * @param size 每页大小
     * @return 分页结果
     */
    @Deprecated
    @GetMapping("/list/chart")
    public BaseResponse<Page<ChartFeedback>> listFeedbackByChart(@RequestParam long chartId,
            @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "10") long size) {
        if (chartId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ChartFeedbackQueryRequest queryRequest = new ChartFeedbackQueryRequest();
        queryRequest.setChartId(chartId);
        queryRequest.setCurrent((int) page);
        queryRequest.setPageSize((int) size);
        
        Page<ChartFeedback> feedbackPage = chartFeedbackService.page(new Page<>(page, size),
                chartFeedbackService.getQueryWrapper(queryRequest));
        return ResultUtils.success(feedbackPage);
    }
}
