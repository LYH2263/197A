package com.shop.service;

import com.shop.entity.BrowseHistory;
import com.shop.mapper.BrowseHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrowseHistoryService {

    private final BrowseHistoryMapper browseHistoryMapper;

    public void recordView(Long userId, Long productId, Long categoryId) {
        if (userId == null) return;
        browseHistoryMapper.insertOrUpdate(userId, productId, categoryId);
    }

    public List<BrowseHistory> getRecentByCategory(Long userId, Long categoryId, int limit) {
        if (userId == null) return List.of();
        return browseHistoryMapper.selectByUserAndCategory(userId, categoryId, limit);
    }

    public List<Long> getRecentProductIds(Long userId, int limit) {
        if (userId == null) return List.of();
        return browseHistoryMapper.selectRecentProductIdsByUser(userId, limit);
    }
}
