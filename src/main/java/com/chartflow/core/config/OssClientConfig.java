package com.chartflow.core.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云对象存储客户端配置
 *
 */
@Configuration
@ConfigurationProperties(prefix = "oss")
@Data
@Slf4j
public class OssClientConfig {

    /**
     * accessKey
     */
    private String accessKey;

    /**
     * secretKey
     */
    private String secretKey;

    /**
     * endpoint
     */
    private String endpoint;

    /**
     * 桶名
     */
    private String bucket;

    @Bean
    public OSS ossClient() {
        // 检查配置是否有效
        if (isConfigInvalid()) {
            log.warn("OSS配置未正确设置，将跳过OSS客户端初始化");
            return null;
        }
        try {
            return new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        } catch (Exception e) {
            log.error("创建OSS客户端失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查OSS配置是否有效
     */
    public boolean isConfigInvalid() {
        return accessKey == null || accessKey.isEmpty() || 
               accessKey.equals("your-access-key") ||
               secretKey == null || secretKey.isEmpty() ||
               secretKey.equals("your-secret-key");
    }
}
