package com.shop.controller;

import com.shop.common.Result;
import com.shop.entity.Product;
import com.shop.service.RecommendationService;
import com.shop.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final SiteConfigService siteConfigService;

    @GetMapping("/recommendations/guess-you-like")
    public Result<List<Product>> guessYouLike(
            @RequestParam Long categoryId,
            @RequestParam(required = false) Long excludeProductId) {
        return Result.ok(recommendationService.guessYouLike(categoryId, excludeProductId));
    }

    @GetMapping("/recommendations/viewed-also-view")
    public Result<List<Product>> viewedAlsoView(
            @RequestParam Long categoryId,
            @RequestParam(required = false) Long excludeProductId,
            Authentication auth) {
        Long userId = extractUserId(auth);
        return Result.ok(recommendationService.viewedAlsoView(userId, categoryId, excludeProductId));
    }

    @GetMapping("/recommendations/home")
    public Result<List<Product>> homeRecommendation() {
        return Result.ok(recommendationService.homeGuessYouLike());
    }

    @GetMapping("/recommendations/config")
    public Result<SiteConfigService.RecommendationConfig> getConfig() {
        return Result.ok(siteConfigService.getRecommendationConfig());
    }

    @PutMapping("/admin/recommendations/config")
    public Result<Void> updateConfig(@RequestBody SiteConfigService.RecommendationConfig config) {
        if (config.getGuessYouLikeCount() < 1) config.setGuessYouLikeCount(6);
        if (config.getGuessYouLikeCount() > 50) config.setGuessYouLikeCount(50);
        if (config.getViewedAlsoViewCount() < 1) config.setViewedAlsoViewCount(6);
        if (config.getViewedAlsoViewCount() > 50) config.setViewedAlsoViewCount(50);
        siteConfigService.setRecommendationConfig(config);
        siteConfigService.clearCache();
        recommendationService.clearCache();
        return Result.ok();
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }
}
