package com.chartflow.core.config;

import com.chartflow.core.service.ChartImageService;
import com.chartflow.core.utils.EmailContentBuilder;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class EmailConfig {

    private final ChartImageService chartImageService;

    @PostConstruct
    public void init() {
        // 设置 EmailContentBuilder 的 ChartImageService
        EmailContentBuilder.setChartImageService(chartImageService);
    }
}