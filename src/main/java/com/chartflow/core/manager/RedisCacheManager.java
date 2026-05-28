package com.chartflow.core.manager;

import com.alibaba.fastjson.JSON;
import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.model.entity.Prompt;
import com.chartflow.core.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存管理器
 * 提供通用的缓存操作能力
 */
@Slf4j
@Service
public class RedisCacheManager {

    @Resource
    private RedissonClient redissonClient;

    // ==================== 缓存键前缀 ====================
    
    private static final String CHART_CACHE_PREFIX = "chart:";
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String PROMPT_CACHE_PREFIX = "prompt:";

    // ==================== 缓存时间配置 ====================
    
    private static final long CHART_CACHE_MINUTES = 60;
    private static final long USER_CACHE_MINUTES = 30;
    private static final long PROMPT_CACHE_MINUTES = 120;

    // ==================== 图表缓存操作 ====================

    public void cacheChart(Chart chart) {
        if (chart == null || chart.getId() == null) {
            return;
        }
        String key = CHART_CACHE_PREFIX + chart.getId();
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(JSON.toJSONString(chart), CHART_CACHE_MINUTES, TimeUnit.MINUTES);
            log.debug("缓存图表成功: chartId={}", chart.getId());
        } catch (Exception e) {
            log.error("缓存图表失败: chartId={}, error={}", chart.getId(), e.getMessage());
        }
    }

    public Chart getCachedChart(Long chartId) {
        if (chartId == null) {
            return null;
        }
        String key = CHART_CACHE_PREFIX + chartId;
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            String value = bucket.get();
            if (value != null) {
                log.debug("从缓存获取图表成功: chartId={}", chartId);
                return JSON.parseObject(value, Chart.class);
            }
        } catch (Exception e) {
            log.error("从缓存获取图表失败: chartId={}, error={}", chartId, e.getMessage());
        }
        return null;
    }

    public void removeChartCache(Long chartId) {
        if (chartId == null) {
            return;
        }
        String key = CHART_CACHE_PREFIX + chartId;
        try {
            redissonClient.getBucket(key).delete();
            log.debug("删除图表缓存: chartId={}", chartId);
        } catch (Exception e) {
            log.error("删除图表缓存失败: chartId={}, error={}", chartId, e.getMessage());
        }
    }

    // ==================== 用户缓存操作 ====================

    public void cacheUser(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String key = USER_CACHE_PREFIX + user.getId();
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(JSON.toJSONString(user), USER_CACHE_MINUTES, TimeUnit.MINUTES);
            log.debug("缓存用户成功: userId={}", user.getId());
        } catch (Exception e) {
            log.error("缓存用户失败: userId={}, error={}", user.getId(), e.getMessage());
        }
    }

    public User getCachedUser(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = USER_CACHE_PREFIX + userId;
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            String value = bucket.get();
            if (value != null) {
                log.debug("从缓存获取用户成功: userId={}", userId);
                return JSON.parseObject(value, User.class);
            }
        } catch (Exception e) {
            log.error("从缓存获取用户失败: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    public void removeUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        String key = USER_CACHE_PREFIX + userId;
        try {
            redissonClient.getBucket(key).delete();
            log.debug("删除用户缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("删除用户缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    // ==================== Prompt缓存操作 ====================

    public void cachePrompt(Prompt prompt) {
        if (prompt == null || prompt.getId() == null) {
            return;
        }
        String key = PROMPT_CACHE_PREFIX + prompt.getId();
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(JSON.toJSONString(prompt), PROMPT_CACHE_MINUTES, TimeUnit.MINUTES);
            log.debug("缓存Prompt成功: promptId={}", prompt.getId());
        } catch (Exception e) {
            log.error("缓存Prompt失败: promptId={}, error={}", prompt.getId(), e.getMessage());
        }
    }

    public Prompt getCachedPrompt(Long promptId) {
        if (promptId == null) {
            return null;
        }
        String key = PROMPT_CACHE_PREFIX + promptId;
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            String value = bucket.get();
            if (value != null) {
                log.debug("从缓存获取Prompt成功: promptId={}", promptId);
                return JSON.parseObject(value, Prompt.class);
            }
        } catch (Exception e) {
            log.error("从缓存获取Prompt失败: promptId={}, error={}", promptId, e.getMessage());
        }
        return null;
    }

    public void removePromptCache(Long promptId) {
        if (promptId == null) {
            return;
        }
        String key = PROMPT_CACHE_PREFIX + promptId;
        try {
            redissonClient.getBucket(key).delete();
            log.debug("删除Prompt缓存: promptId={}", promptId);
        } catch (Exception e) {
            log.error("删除Prompt缓存失败: promptId={}, error={}", promptId, e.getMessage());
        }
    }
}