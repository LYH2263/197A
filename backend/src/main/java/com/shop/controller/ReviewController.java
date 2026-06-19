package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.ReviewRequest;
import com.shop.dto.ReviewVO;
import com.shop.entity.Review;
import com.shop.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品评价接口
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping
    public Result<Review> create(Authentication auth, @Valid @RequestBody ReviewRequest req) {
        Long userId = requireUserId(auth);
        Review review = reviewService.create(userId, req);
        return Result.ok(review);
    }

    @PutMapping("/{id}")
    public Result<Review> update(Authentication auth, @PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        Long userId = requireUserId(auth);
        Review review = reviewService.update(userId, id, req);
        return Result.ok(review);
    }

    @GetMapping("/product/{productId}")
    public Result<List<ReviewVO>> listByProduct(@PathVariable Long productId, Authentication auth) {
        Long currentUserId = null;
        if (auth != null && auth.getPrincipal() instanceof Long) {
            currentUserId = (Long) auth.getPrincipal();
        }
        return Result.ok(reviewService.listByProductIdWithTimeline(productId, currentUserId));
    }

    /** 我的评价 */
    @GetMapping("/me")
    public Result<List<ReviewVO>> myReviews(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(reviewService.listByUserId(userId));
    }
}
