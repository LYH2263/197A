package com.shop.controller;

import com.shop.common.Result;
import com.shop.entity.PriceAlert;
import com.shop.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/price-alert")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<List<PriceAlert>> list(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(priceAlertService.listByUserId(userId));
    }

    @PostMapping("/subscribe")
    public Result<Void> subscribe(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        BigDecimal targetPrice = new BigDecimal(body.get("targetPrice").toString());
        priceAlertService.subscribe(userId, productId, targetPrice);
        return Result.ok();
    }

    @DeleteMapping("/{productId}")
    public Result<Void> unsubscribe(Authentication auth, @PathVariable Long productId) {
        Long userId = requireUserId(auth);
        priceAlertService.unsubscribe(userId, productId);
        return Result.ok();
    }
}
