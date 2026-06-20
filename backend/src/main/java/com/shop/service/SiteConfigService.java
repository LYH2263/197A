package com.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.entity.SiteConfig;
import com.shop.mapper.SiteConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteConfigService {

    private final SiteConfigMapper siteConfigMapper;
    private final ObjectMapper objectMapper;

    private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private static class CachedEntry {
        final String value;
        final long timestamp;
        CachedEntry(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public String getRawValue(String key) {
        CachedEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        SiteConfig config = siteConfigMapper.selectByKey(key);
        if (config == null) return null;
        cache.put(key, new CachedEntry(config.getConfigValue()));
        return config.getConfigValue();
    }

    public <T> T getConfig(String key, Class<T> clazz, T defaultValue) {
        String raw = getRawValue(key);
        if (raw == null) return defaultValue;
        try {
            return objectMapper.readValue(raw, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse site_config key={}", key, e);
            return defaultValue;
        }
    }

    public void setConfig(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            siteConfigMapper.upsert(key, json);
            cache.remove(key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize site_config key={}", key, e);
            throw new RuntimeException("配置序列化失败", e);
        }
    }

    public RecommendationConfig getRecommendationConfig() {
        return getConfig("recommendation", RecommendationConfig.class, new RecommendationConfig());
    }

    public void setRecommendationConfig(RecommendationConfig config) {
        setConfig("recommendation", config);
    }

    public void clearCache() {
        cache.clear();
    }

    @lombok.Data
    public static class RecommendationConfig {
        private boolean guessYouLikeEnabled = true;
        private int guessYouLikeCount = 6;
        private boolean viewedAlsoViewEnabled = true;
        private int viewedAlsoViewCount = 6;
    }
}
