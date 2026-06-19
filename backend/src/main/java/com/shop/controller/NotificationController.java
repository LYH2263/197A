package com.shop.controller;

import com.shop.common.Result;
import com.shop.entity.Notification;
import com.shop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<List<Notification>> list(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(notificationService.listByUserId(userId));
    }

    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(notificationService.countUnread(userId));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        notificationService.markRead(id, userId);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(Authentication auth) {
        Long userId = requireUserId(auth);
        notificationService.markAllRead(userId);
        return Result.ok();
    }
}
