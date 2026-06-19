package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.ReviewVO;
import com.shop.dto.ShipRequest;
import com.shop.dto.UserVO;
import com.shop.entity.OrderMain;
import com.shop.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    private Long requireAdminId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/users")
    public Result<List<UserVO>> listUsers() {
        return Result.ok(adminService.listUsers());
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        String role = body.get("role") != null ? String.valueOf(body.get("role")) : null;
        adminService.updateUser(id, status, role);
        return Result.ok();
    }

    @GetMapping("/reviews")
    public Result<List<ReviewVO>> listReviews() {
        return Result.ok(adminService.listAllReviews());
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        adminService.deleteReview(id);
        return Result.ok();
    }

    @GetMapping("/orders")
    public Result<List<OrderMain>> listOrders(@RequestParam(required = false) Integer status) {
        return Result.ok(adminService.listOrdersByStatus(status));
    }

    @GetMapping("/orders/pending-ship")
    public Result<List<OrderMain>> listPendingShipOrders() {
        return Result.ok(adminService.listOrdersByStatus(1));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderMain> getOrder(@PathVariable Long id) {
        OrderMain order = adminService.getOrderById(id);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.ok(order);
    }

    @PostMapping("/orders/ship")
    public Result<Void> ship(Authentication auth, @RequestBody ShipRequest req) {
        Long adminId = requireAdminId(auth);
        String adminName = auth.getName() != null ? auth.getName() : null;
        adminService.ship(req, adminId, adminName);
        return Result.ok();
    }
}
