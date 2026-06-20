package com.shop.service;

import com.shop.entity.Product;
import com.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ProductMapper productMapper;
    private final BrowseHistoryService browseHistoryService;
    private final SiteConfigService siteConfigService;

    private final Map<String, CachedRecommendation> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private static class CachedRecommendation {
        final List<Product> products;
        final long timestamp;
        CachedRecommendation(List<Product> products) {
            this.products = products;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public List<Product> guessYouLike(Long categoryId, Long excludeProductId) {
        SiteConfigService.RecommendationConfig config = siteConfigService.getRecommendationConfig();
        if (!config.isGuessYouLikeEnabled()) {
            clearCategoryCache(categoryId, "guess:");
            return List.of();
        }
        int limit = config.getGuessYouLikeCount();

        String cacheKey = "guess:" + categoryId + ":" + excludeProductId;
        CachedRecommendation cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.products;
        }

        List<Product> products = productMapper.selectByCategoryOrderBySales(categoryId, excludeProductId, limit);
        cache.put(cacheKey, new CachedRecommendation(products));
        return products;
    }

    public List<Product> viewedAlsoView(Long userId, Long categoryId, Long excludeProductId) {
        SiteConfigService.RecommendationConfig config = siteConfigService.getRecommendationConfig();
        if (!config.isViewedAlsoViewEnabled()) {
            clearCategoryCache(categoryId, "viewed:");
            return List.of();
        }
        int limit = config.getViewedAlsoViewCount();

        String cacheKey = "viewed:" + userId + ":" + categoryId + ":" + excludeProductId;
        CachedRecommendation cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.products;
        }

        List<Product> result;
        if (userId != null) {
            List<Long> recentIds = browseHistoryService.getRecentByCategory(userId, categoryId, limit + 1)
                    .stream()
                    .map(b -> b.getProductId())
                    .filter(id -> !id.equals(excludeProductId))
                    .collect(Collectors.toList());

            if (!recentIds.isEmpty()) {
                List<Product> products = productMapper.selectByIds(recentIds);
                Map<Long, Product> productMap = products.stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));
                result = recentIds.stream()
                        .map(productMap::get)
                        .filter(Objects::nonNull)
                        .limit(limit)
                        .collect(Collectors.toList());
            } else {
                result = productMapper.selectRandomByCategory(categoryId, excludeProductId, limit);
            }
        } else {
            result = productMapper.selectRandomByCategory(categoryId, excludeProductId, limit);
        }

        cache.put(cacheKey, new CachedRecommendation(result));
        return result;
    }

    public List<Product> homeGuessYouLike() {
        SiteConfigService.RecommendationConfig config = siteConfigService.getRecommendationConfig();
        if (!config.isGuessYouLikeEnabled()) {
            cache.remove("home:guess");
            return List.of();
        }

        String cacheKey = "home:guess";
        CachedRecommendation cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.products;
        }

        List<Product> products = productMapper.selectTopBySales(config.getGuessYouLikeCount());
        cache.put(cacheKey, new CachedRecommendation(products));
        return products;
    }

    private void clearCategoryCache(Long categoryId, String prefix) {
        if (categoryId == null) return;
        Iterator<Map.Entry<String, CachedRecommendation>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CachedRecommendation> e = it.next();
            if (e.getKey().startsWith(prefix) && e.getKey().contains(":" + categoryId + ":")) {
                it.remove();
            }
        }
    }

    public void clearCache() {
        cache.clear();
    }
}
