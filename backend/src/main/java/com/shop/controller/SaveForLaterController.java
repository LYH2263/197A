package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.SaveForLaterVO;
import com.shop.service.SaveForLaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/save-for-later")
@RequiredArgsConstructor
public class SaveForLaterController {

    private final SaveForLaterService saveForLaterService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<List<SaveForLaterVO>> list(Authentication auth,
                                              @RequestParam(defaultValue = "time") String sortBy) {
        Long userId = requireUserId(auth);
        return Result.ok(saveForLaterService.list(userId, sortBy));
    }

    @GetMapping("/count")
    public Result<Integer> count(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(saveForLaterService.countByUserId(userId));
    }

    @PostMapping("/move")
    public Result<Void> moveToSaveForLater(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        saveForLaterService.moveToSaveForLater(userId, productId);
        return Result.ok();
    }

    @PostMapping("/move-back")
    public Result<Void> moveBackToCart(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        saveForLaterService.moveBackToCart(userId, productId);
        return Result.ok();
    }

    @PostMapping("/batch-move-back")
    public Result<Void> batchMoveBackToCart(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        @SuppressWarnings("unchecked")
        List<Long> productIds = ((List<Number>) body.get("productIds")).stream()
                .map(Number::longValue).toList();
        saveForLaterService.batchMoveToCart(userId, productIds);
        return Result.ok();
    }

    @DeleteMapping("/{productId}")
    public Result<Void> remove(Authentication auth, @PathVariable Long productId) {
        Long userId = requireUserId(auth);
        saveForLaterService.remove(userId, productId);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        @SuppressWarnings("unchecked")
        List<Long> productIds = ((List<Number>) body.get("productIds")).stream()
                .map(Number::longValue).toList();
        saveForLaterService.batchDelete(userId, productIds);
        return Result.ok();
    }
}
