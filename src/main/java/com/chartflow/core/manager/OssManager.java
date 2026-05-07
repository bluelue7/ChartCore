package com.chartflow.core.manager;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.chartflow.core.common.ErrorCode;
import com.chartflow.core.config.OssClientConfig;
import com.chartflow.core.exception.BusinessException;
import java.io.File;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OSS 对象存储操作
 *
 */
@Component
@Slf4j
public class OssManager {

    @Resource
    private OssClientConfig ossClientConfig;

    @Resource
    private OSS ossClient;

    /**
     * 上传对象
     *
     * @param key 唯一键
     * @param localFilePath 本地文件路径
     * @return
     */
    public PutObjectResult putObject(String key, String localFilePath) {
        validateOssClient();
        PutObjectRequest putObjectRequest = new PutObjectRequest(ossClientConfig.getBucket(), key,
                new File(localFilePath));
        return ossClient.putObject(putObjectRequest);
    }

    /**
     * 上传对象
     *
     * @param key 唯一键
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        validateOssClient();
        PutObjectRequest putObjectRequest = new PutObjectRequest(ossClientConfig.getBucket(), key,
                file);
        return ossClient.putObject(putObjectRequest);
    }

    /**
     * 获取Bucket名称
     *
     * @return
     */
    public String getBucket() {
        return ossClientConfig.getBucket();
    }

    /**
     * 校验OSS客户端是否可用
     */
    private void validateOssClient() {
        if (ossClient == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "OSS客户端未正确配置，请检查OSS相关配置");
        }
    }
}
