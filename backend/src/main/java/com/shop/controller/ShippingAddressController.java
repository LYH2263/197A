package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.ShippingAddressRequest;
import com.shop.entity.ShippingAddress;
import com.shop.service.ShippingAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<List<ShippingAddress>> list(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(shippingAddressService.listByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<ShippingAddress> getById(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        ShippingAddress addr = shippingAddressService.getById(id, userId);
        if (addr == null) {
            return Result.fail(404, "地址不存在");
        }
        return Result.ok(addr);
    }

    @GetMapping("/default")
    public Result<ShippingAddress> getDefault(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(shippingAddressService.getDefault(userId));
    }

    @PostMapping
    public Result<ShippingAddress> create(Authentication auth, @Valid @RequestBody ShippingAddressRequest req) {
        Long userId = requireUserId(auth);
        return Result.ok(shippingAddressService.create(userId, req));
    }

    @PutMapping("/{id}")
    public Result<ShippingAddress> update(Authentication auth, @PathVariable Long id, @Valid @RequestBody ShippingAddressRequest req) {
        Long userId = requireUserId(auth);
        return Result.ok(shippingAddressService.update(id, userId, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        shippingAddressService.delete(id, userId);
        return Result.ok();
    }

    @PostMapping("/{id}/default")
    public Result<Void> setDefault(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        shippingAddressService.setDefault(id, userId);
        return Result.ok();
    }
}
