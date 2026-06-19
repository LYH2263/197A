package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.LoginRequest;
import com.shop.dto.RegisterRequest;
import com.shop.dto.UserVO;
import com.shop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口：登录、注册、当前用户
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        String token = authService.register(req);
        return Result.ok(new LoginResponse(token));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req);
        return Result.ok(new LoginResponse(token));
    }

    @GetMapping("/me")
    public Result<UserVO> me(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) auth.getPrincipal();
        UserVO user = authService.getCurrentUser(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        return Result.ok(user);
    }

    @lombok.Data
    public static class LoginResponse {
        private final String token;
    }
}
