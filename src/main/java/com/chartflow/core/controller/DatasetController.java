package com.chartflow.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chartflow.core.common.BaseResponse;
import com.chartflow.core.common.ResultUtils;
import com.chartflow.core.model.dto.dataset.DatasetQueryRequest;
import com.chartflow.core.model.entity.Dataset;
import com.chartflow.core.model.entity.User;
import com.chartflow.core.service.DatasetService;
import com.chartflow.core.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 数据集接口
 *
 */
@RestController
@RequestMapping("/dataset")
@Slf4j
public class DatasetController {

    @Resource
    private DatasetService datasetService;

    @Resource
    private UserService userService;

    /**
     * 上传数据集文件
     *
     * @param file 用户上传的文件
     * @param request 请求对象
     * @return 保存的数据集信息
     */
    @PostMapping("/upload")
    public BaseResponse<Dataset> uploadDataset(@RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Dataset dataset = datasetService.uploadAndSave(file, loginUser.getId());
        return ResultUtils.success(dataset);
    }

    /**
     * 分页获取数据集列表
     *
     * @param datasetQueryRequest 查询请求
     * @param request 请求对象
     * @return 分页结果
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Dataset>> listDatasetByPage(@RequestBody DatasetQueryRequest datasetQueryRequest,
            HttpServletRequest request) {
        long current = datasetQueryRequest.getCurrent();
        long size = datasetQueryRequest.getPageSize();
        Page<Dataset> datasetPage = datasetService.page(new Page<>(current, size),
                datasetService.getQueryWrapper(datasetQueryRequest));
        return ResultUtils.success(datasetPage);
    }

    /**
     * 获取当前用户的数据集列表（简化接口）
     *
     * @param page 当前页码
     * @param size 每页大小
     * @param request 请求对象
     * @return 分页结果
     */
    @GetMapping("/list/my")
    public BaseResponse<Page<Dataset>> listMyDataset(@RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        DatasetQueryRequest queryRequest = new DatasetQueryRequest();
        queryRequest.setUserId(loginUser.getId());
        queryRequest.setCurrent((int) page);
        queryRequest.setPageSize((int) size);
        Page<Dataset> datasetPage = datasetService.page(new Page<>(page, size),
                datasetService.getQueryWrapper(queryRequest));
        return ResultUtils.success(datasetPage);
    }

    /**
     * 根据ID获取数据集详情
     *
     * @param id 数据集ID
     * @param request 请求对象
     * @return 数据集详情
     */
    @GetMapping("/get")
    public BaseResponse<Dataset> getDatasetById(@RequestParam long id, HttpServletRequest request) {
        Dataset dataset = datasetService.getById(id);
        if (dataset == null) {
            return ResultUtils.error(404, "数据集不存在");
        }
        return ResultUtils.success(dataset);
    }

    /**
     * 删除数据集
     *
     * @param id 数据集ID
     * @param request 请求对象
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteDataset(@RequestParam long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Dataset dataset = datasetService.getById(id);
        if (dataset == null) {
            return ResultUtils.error(404, "数据集不存在");
        }
        // 只有管理员或数据集所有者可以删除
        if (!loginUser.getId().equals(dataset.getUserId())) {
            return ResultUtils.error(403, "无权限删除");
        }
        boolean result = datasetService.removeById(id);
        return ResultUtils.success(result);
    }
}
