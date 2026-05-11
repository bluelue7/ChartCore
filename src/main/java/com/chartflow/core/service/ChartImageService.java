package com.chartflow.core.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
public class ChartImageService {

    @Value("${oss.endpoint}")
    private String ossEndpoint;

    @Value("${oss.accesskey}")
    private String ossAccessKey;

    @Value("${oss.secretkey}")
    private String ossSecretKey;

    @Value("${oss.bucket}")
    private String ossBucket;

    @Value("${oss.chart-folder:charts/}")
    private String ossChartFolder;

    /**
     * 使用 Node.js 生成图表图片并上传到 OSS
     * @param chartOption - ECharts 配置 JSON
     * @return - OSS 图片 URL
     */
    public String generateAndUploadChart(String chartOption) {
        try {
            // 1. 调用 Node.js 脚本生成图片
            byte[] imageData = generateChartImage(chartOption);
            if (imageData == null || imageData.length == 0) {
                log.error("图表图片生成失败");
                return null;
            }

            // 2. 上传到 OSS
            String fileName = UUID.randomUUID().toString() + ".png";
            String ossPath = ossChartFolder + fileName;
            
            return uploadToOss(imageData, ossPath);

        } catch (Exception e) {
            log.error("生成并上传图表失败", e);
            return null;
        }
    }

    /**
     * 调用 Node.js 脚本生成图表
     */
    private byte[] generateChartImage(String chartOption) throws IOException, InterruptedException {
        log.info("chartOption length: {}", chartOption.length());
        log.info("chartOption first 100 chars: {}", chartOption.substring(0, Math.min(100, chartOption.length())));

        String scriptPath = "./mcp_server/gene" +
                "rate_chart.js";

        ProcessBuilder pb = new ProcessBuilder("node", scriptPath, chartOption);
        pb.redirectErrorStream(false); // 分开处理错误输出

        Process process = pb.start();

        // 读取标准输出（图片数据）
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        // 读取错误输出
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("Node.js 错误: {}", errorOutput.toString());
            return null;
        }

        // 如果有错误输出但退出码为0，也记录一下
        if (errorOutput.length() > 0) {
            log.warn("Node.js 警告: {}", errorOutput.toString());
        }
        
        return outputStream.toByteArray();
    }

    /**
     * 上传图片到 OSS
     */
    private String uploadToOss(byte[] data, String ossPath) {
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(
                    ossEndpoint,
                    ossAccessKey,
                    ossSecretKey
            );

            ossClient.putObject(ossBucket, ossPath, new ByteArrayInputStream(data));
            
            // 返回公开访问 URL
            return String.format("https://%s.%s/%s", ossBucket, ossEndpoint, ossPath);

        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}