package com.chartflow.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chartflow.core.common.BaseResponse;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.common.ResultUtils;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.model.dto.prompt.PromptAddRequest;
import com.chartflow.core.model.dto.prompt.PromptQueryRequest;
import com.chartflow.core.model.dto.prompt.PromptUpdateRequest;
import com.chartflow.core.model.entity.Prompt;
import com.chartflow.core.model.entity.User;
import com.chartflow.core.service.PromptService;
import com.chartflow.core.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
 * 分析模板接口
 *
 */
@RestController
@RequestMapping("/prompt")
@Slf4j
public class PromptController {

    @Resource
    private PromptService promptService;

    @Resource
    private UserService userService;

    /**
     * 添加模板
     *
     * @param request 模板添加请求
     * @param httpRequest 请求对象
     * @return 模板ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addPrompt(@RequestBody PromptAddRequest request, HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isAnyBlank(request.getName(), request.getPromptQuery())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模板名称和查询内容不能为空");
        }
        
        User loginUser = userService.getLoginUser(httpRequest);
        
        Prompt prompt = new Prompt();
        BeanUtils.copyProperties(request, prompt);
        prompt.setUserId(loginUser.getId());
        prompt.setUsageCount(0);
        
        boolean result = promptService.save(prompt);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存模板失败");
        }
        return ResultUtils.success(prompt.getId());
    }

    /**
     * 删除模板
     *
     * @param id 模板ID
     * @param httpRequest 请求对象
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deletePrompt(@RequestParam long id, HttpServletRequest httpRequest) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        
        Prompt prompt = promptService.getById(id);
        if (prompt == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "模板不存在");
        }
        
        // 系统预置模板不能删除
        if (prompt.getUserId() == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "系统预置模板不能删除");
        }
        
        // 只有管理员或模板所有者可以删除
        if (!loginUser.getId().equals(prompt.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除");
        }
        
        // 使用 MyBatis-Plus 的 removeById 方法，自动处理逻辑删除
        boolean result = promptService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新模板
     *
     * @param request 模板更新请求
     * @param httpRequest 请求对象
     * @return 更新结果
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updatePrompt(@RequestBody PromptUpdateRequest request,
            HttpServletRequest httpRequest) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        User loginUser = userService.getLoginUser(httpRequest);
        Long id = request.getId();
        
        Prompt prompt = promptService.getById(id);
        if (prompt == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "模板不存在");
        }
        
        // 系统预置模板不能修改
        if (prompt.getUserId() == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "系统预置模板不能修改");
        }
        
        // 只有管理员或模板所有者可以更新
        if (!loginUser.getId().equals(prompt.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限更新");
        }
        
        if (StringUtils.isAnyBlank(request.getName(), request.getPromptQuery())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模板名称和查询内容不能为空");
        }

        Prompt updatePrompt = new Prompt();
        BeanUtils.copyProperties(request, updatePrompt);
        
        boolean result = promptService.updateById(updatePrompt);
        return ResultUtils.success(result);
    }

    /**
     * 根据ID获取模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/get")
    public BaseResponse<Prompt> getPromptById(@RequestParam long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Prompt prompt = promptService.getById(id);
        if (prompt == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "模板不存在");
        }
        return ResultUtils.success(prompt);
    }

    /**
     * 分页获取模板列表
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Prompt>> listPromptByPage(@RequestBody PromptQueryRequest queryRequest) {
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<Prompt> page = promptService.page(new Page<>(current, size),
                promptService.getQueryWrapper(queryRequest));
        return ResultUtils.success(page);
    }

    /**
     * 获取当前用户的模板列表
     *
     * @param page 当前页码
     * @param size 每页大小
     * @param httpRequest 请求对象
     * @return 分页结果
     */
    @GetMapping("/list/my")
    public BaseResponse<Page<Prompt>> listMyPrompt(@RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        PromptQueryRequest queryRequest = new PromptQueryRequest();
        queryRequest.setUserId(loginUser.getId());
        queryRequest.setCurrent((int) page);
        queryRequest.setPageSize((int) size);
        
        Page<Prompt> promptPage = promptService.page(new Page<>(page, size),
                promptService.getQueryWrapper(queryRequest));
        return ResultUtils.success(promptPage);
    }

    /**
     * 获取系统预置模板和用户模板列表
     *
     * @param httpRequest 请求对象
     * @return 所有可用模板
     */
    @GetMapping("/list/all")
    public BaseResponse<Page<Prompt>> listAllPrompt(@RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        
        // 查询系统预置模板（userId为null）和当前用户的模板
        PromptQueryRequest queryRequest = new PromptQueryRequest();
        queryRequest.setCurrent((int) page);
        queryRequest.setPageSize((int) size);
        
        Page<Prompt> promptPage = promptService.page(new Page<>(page, size),
                promptService.getQueryWrapper(queryRequest).and(wrapper -> 
                    wrapper.isNull("userId").or().eq("userId", loginUser.getId())
                ));
        return ResultUtils.success(promptPage);
    }
}
