package com.shop.service;

import com.shop.entity.Notification;
import com.shop.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public List<Notification> listByUserId(Long userId) {
        return notificationMapper.selectByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id, Long userId) {
        notificationMapper.markRead(id, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }

    public int countUnread(Long userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }
}
