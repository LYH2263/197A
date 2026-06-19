package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.CartItemVO;
import com.shop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<List<CartItemVO>> list(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(cartService.list(userId));
    }

    @PostMapping("/add")
    public Result<Void> add(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        int quantity = body.get("quantity") != null ? ((Number) body.get("quantity")).intValue() : 1;
        cartService.add(userId, productId, quantity);
        return Result.ok();
    }

    @PutMapping("/quantity")
    public Result<Void> updateQuantity(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        int quantity = ((Number) body.get("quantity")).intValue();
        cartService.updateQuantity(userId, productId, quantity);
        return Result.ok();
    }

    @PutMapping("/checked")
    public Result<Void> setChecked(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        int checked = ((Number) body.get("checked")).intValue();
        cartService.setChecked(userId, productId, checked);
        return Result.ok();
    }

    @DeleteMapping("/{productId}")
    public Result<Void> remove(Authentication auth, @PathVariable Long productId) {
        Long userId = requireUserId(auth);
        cartService.remove(userId, productId);
        return Result.ok();
    }
}
