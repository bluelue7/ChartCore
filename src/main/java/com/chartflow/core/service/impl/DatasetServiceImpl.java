package com.chartflow.core.service.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.constant.CommonConstant;
import com.chartflow.core.exception.BusinessException;
import com.chartflow.core.manager.OssManager;
import com.chartflow.core.mapper.DatasetMapper;
import com.chartflow.core.model.dto.dataset.DatasetQueryRequest;
import com.chartflow.core.model.entity.Dataset;
import com.chartflow.core.service.DatasetService;
import com.chartflow.core.utils.SqlUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
* @author bluelue7
* @description 针对表【dataset(数据集表)】的数据库操作Service实现
* @createDate 2026-05-06
*/
@Service
@Slf4j
public class DatasetServiceImpl extends ServiceImpl<DatasetMapper, Dataset>
    implements DatasetService{

    @Resource
    private OssManager ossManager;

    private static final String DATASET_DIR = "dataset";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Dataset uploadAndSave(MultipartFile file, Long userId) {
        // 校验文件
        validFile(file);

        // 生成文件名
        String originalFilename = file.getOriginalFilename();

        // 检查是否存在同名文件（同一用户）
        Dataset existDataset = checkDuplicateFile(userId, originalFilename);
        if (existDataset != null) {
            log.info("数据集已存在: userId={}, datasetId={}, fileName={}",
                    userId, existDataset.getId(), originalFilename);
            return existDataset;
        }

        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + originalFilename;
        String ossKey = String.format("%s/%s/%s", DATASET_DIR, userId, filename);

        File tempFile = null;
        try {
            // 创建临时文件
            tempFile = File.createTempFile("dataset_", "." + FileUtil.getSuffix(originalFilename));
            file.transferTo(tempFile);

            // 上传到OSS
            ossManager.putObject(ossKey, tempFile);

            // 解析文件获取元信息
            FileMetaInfo metaInfo = parseFileMetaInfo(tempFile);

            // 构建Dataset实体
            Dataset dataset = new Dataset();
            dataset.setName(originalFilename);
            dataset.setFilePath("https://" + ossManager.getBucket() +"."+ossManager.getEndpoint()+ "/" + ossKey);
            dataset.setRowCount(metaInfo.rowCount);
            dataset.setColumnCount(metaInfo.columnCount);
            dataset.setColumnMeta(metaInfo.columnMeta);
            dataset.setUserId(userId);

            // 保存到数据库
            boolean save = this.save(dataset);
            if (!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存数据集失败");
            }

            log.info("数据集上传成功: userId={}, datasetId={}, filePath={}",
                    userId, dataset.getId(), dataset.getFilePath());
            return dataset;

        } catch (Exception e) {
            log.error("数据集上传失败, ossKey = " + ossKey, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                boolean delete = tempFile.delete();
                if (!delete) {
                    log.error("临时文件删除失败: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 检查是否存在同名文件（同一用户）
     *
     * @param userId 用户ID
     * @param fileName 文件名
     * @return 如果存在则返回已存在的数据集，否则返回null
     */
    private Dataset checkDuplicateFile(Long userId, String fileName) {
        if (userId == null || fileName == null || fileName.isEmpty()) {
            return null;
        }
        QueryWrapper<Dataset> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("name", fileName);
        queryWrapper.eq("isDelete", 0);
        return this.getOne(queryWrapper);
    }

    /**
     * 校验上传的文件
     */
    private void validFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        String suffix = FileUtil.getSuffix(originalFilename).toLowerCase();
        if (!"xlsx".equals(suffix) && !"xls".equals(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只支持XLSX、XLS格式的文件");
        }

        // 文件大小限制 10MB
        long maxSize = 10 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过10MB");
        }
    }

    /**
     * 解析文件元信息
     */
    private FileMetaInfo parseFileMetaInfo(File file) {
        FileMetaInfo metaInfo = new FileMetaInfo();
        metaInfo.rowCount = 0;
        metaInfo.columnCount = 0;
        metaInfo.columnMeta = "[]";

        String suffix = FileUtil.getSuffix(file.getName()).toLowerCase();
        
        try {
           if ("xlsx".equals(suffix) || "xls".equals(suffix)) {
                parseExcelFile(file, metaInfo);
            }
        } catch (Exception e) {
            log.warn("解析文件元信息失败: {}", e.getMessage());
        }

        return metaInfo;
    }


    /**
     * 解析Excel文件
     */
    private void parseExcelFile(File file, FileMetaInfo metaInfo) {
        List<Map<Integer, Object>> dataList = new ArrayList<>();

        EasyExcel.read(file, new AnalysisEventListener<Map<Integer, Object>>() {
            @Override
            public void invoke(Map<Integer, Object> data, AnalysisContext context) {
                dataList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 分析完成
            }
        }).sheet().doRead();

        if (dataList.isEmpty()) {
            return;
        }

        // 获取列数
        Map<Integer, Object> firstRow = dataList.get(0);
        metaInfo.columnCount = firstRow.size();

        // 获取字段名（假设第一行为表头）
        List<String> columnNames = new ArrayList<>();
        for (int i = 0; i < metaInfo.columnCount; i++) {
            Object value = firstRow.get(i);
            columnNames.add(value != null ? value.toString() : "column_" + i);
        }

        // 设置字段元信息
        try {
            metaInfo.columnMeta = objectMapper.writeValueAsString(columnNames);
        } catch (JsonProcessingException e) {
            log.warn("序列化字段元信息失败", e);
        }

        // 行数 = 总行数 - 1（减去表头）
        metaInfo.rowCount = dataList.size() - 1;
    }

    /**
     * 文件元信息内部类
     */
    private static class FileMetaInfo {
        int rowCount;
        int columnCount;
        String columnMeta;
    }

    @Override
    public QueryWrapper<Dataset> getQueryWrapper(DatasetQueryRequest datasetQueryRequest) {
        if (datasetQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long userId = datasetQueryRequest.getUserId();
        String name = datasetQueryRequest.getName();
        Date createTimeStart = datasetQueryRequest.getCreateTimeStart();
        Date createTimeEnd = datasetQueryRequest.getCreateTimeEnd();
        String sortField = datasetQueryRequest.getSortField();
        String sortOrder = datasetQueryRequest.getSortOrder();

        QueryWrapper<Dataset> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.ge(createTimeStart != null, "createTime", createTimeStart);
        queryWrapper.le(createTimeEnd != null, "createTime", createTimeEnd);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
