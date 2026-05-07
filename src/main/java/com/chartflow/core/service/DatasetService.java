package com.chartflow.core.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chartflow.core.model.dto.dataset.DatasetQueryRequest;
import com.chartflow.core.model.entity.Dataset;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
* @author bluelue7
* @description 针对表【dataset(数据集表)】的数据库操作Service
* @createDate 2026-05-06
*/
public interface DatasetService extends IService<Dataset> {

    /**
     * 上传数据集并保存到数据库
     *
     * @param file 用户上传的文件
     * @param userId 用户ID
     * @return 保存的数据集实体
     */
    Dataset uploadAndSave(MultipartFile file, Long userId);

    /**
     * 获取查询条件包装器
     *
     * @param datasetQueryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<Dataset> getQueryWrapper(DatasetQueryRequest datasetQueryRequest);
}
