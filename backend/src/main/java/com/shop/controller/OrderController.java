package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.OrderCreateRequest;
import com.shop.entity.OrderItem;
import com.shop.entity.OrderMain;
import com.shop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping
    public Result<OrderMain> create(Authentication auth, @Valid @RequestBody OrderCreateRequest req) {
        Long userId = requireUserId(auth);
        OrderMain order = orderService.create(userId, req);
        return Result.ok(order);
    }

    @GetMapping
    public Result<List<OrderMain>> list(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(orderService.listByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<OrderMain> getById(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        OrderMain order = orderService.getById(id, userId);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.ok(order);
    }

    @GetMapping("/{id}/items")
    public Result<List<OrderItem>> listItems(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        OrderMain order = orderService.getById(id, userId);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.ok(orderService.listItems(id));
    }

    @PostMapping("/{id}/pay")
    public Result<Void> pay(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        orderService.pay(id, userId);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        orderService.cancel(id, userId);
        return Result.ok();
    }

    @PostMapping("/{id}/receive")
    public Result<Void> receive(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        orderService.receive(id, userId);
        return Result.ok();
    }

    @PostMapping("/{id}/after-sale-intent")
    public Result<Map<String, Object>> afterSaleIntent(Authentication auth,
                                                       @PathVariable Long id,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(auth);
        String source = (body != null && body.get("source") != null) ? String.valueOf(body.get("source")) : "DETAIL";
        Long intentId = orderService.recordAfterSaleIntent(id, userId, source);
        return Result.ok(Map.of("intentId", intentId));
    }

    @PostMapping("/{id}/share-token")
    public Result<Map<String, String>> createShareToken(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        String token = orderService.generateShareToken(id, userId);
        return Result.ok(Map.of("token", token));
    }

    @GetMapping("/share/{token}")
    public Result<Map<String, Object>> getSharedOrder(@PathVariable String token) {
        Map<String, Object> data = orderService.getSharedOrder(token);
        return Result.ok(data);
    }

    @GetMapping("/{id}/timeline")
    public Result<List<Map<String, Object>>> getTimeline(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        OrderMain order = orderService.getById(id, userId);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        List<com.shop.entity.OrderOperationLog> logs = orderService.listOperationLogs(id);
        List<Map<String, Object>> timeline = new java.util.ArrayList<>();
        if (order.getCreatedAt() != null) {
            timeline.add(Map.of("label", "下单", "time", order.getCreatedAt().toString()));
        }
        for (com.shop.entity.OrderOperationLog l : logs) {
            if ("PAY".equals(l.getOperation())) {
                timeline.add(Map.of("label", "支付", "time", l.getCreatedAt() != null ? l.getCreatedAt().toString() : ""));
            }
        }
        if (order.getStatus() >= 2 && order.getShippedAt() != null) {
            timeline.add(Map.of("label", "发货", "time", order.getShippedAt().toString()));
        }
        return Result.ok(timeline);
    }
}
