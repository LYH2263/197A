package com.shop.service;

import com.shop.dto.SaveForLaterVO;
import com.shop.entity.CartItem;
import com.shop.entity.PriceAlert;
import com.shop.entity.Product;
import com.shop.entity.SaveForLater;
import com.shop.mapper.CartItemMapper;
import com.shop.mapper.PriceAlertMapper;
import com.shop.mapper.ProductMapper;
import com.shop.mapper.SaveForLaterMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SaveForLaterService {

    private static final Logger log = LoggerFactory.getLogger(SaveForLaterService.class);

    private final SaveForLaterMapper saveForLaterMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final PriceAlertMapper priceAlertMapper;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> moveToSaveForLater(Long userId, Long productId) {
        CartItem cartItem = cartItemMapper.selectByUserIdAndProductId(userId, productId);
        if (cartItem == null) {
            throw new IllegalArgumentException("购物车中无该商品");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        cartItemMapper.updateChecked(userId, productId, 0);

        SaveForLater existing = saveForLaterMapper.selectByUserIdAndProductId(userId, productId);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + cartItem.getQuantity());
            existing.setPriceSnapshot(product.getPrice());
            saveForLaterMapper.updateById(existing);
        } else {
            SaveForLater item = new SaveForLater();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(cartItem.getQuantity());
            item.setPriceSnapshot(product.getPrice());
            saveForLaterMapper.insert(item);
        }

        cartItemMapper.deleteByUserIdAndProductId(userId, productId);
        log.info("Moved to save-for-later: userId={}, productId={}", userId, productId);

        Map<String, Object> result = new HashMap<>();
        boolean isOffline = product.getStatus() == null || product.getStatus() != 1;
        if (isOffline) {
            result.put("message", "商品「" + product.getName() + "」已下架，移入稍后购买后将无法直接购买");
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchMoveToCart(Long userId, List<Long> productIds) {
        List<String> messages = new ArrayList<>();
        int movedCount = 0;
        for (Long productId : productIds) {
            try {
                Map<String, Object> result = moveBackToCart(userId, productId);
                movedCount++;
                if (result.get("message") != null) {
                    messages.add((String) result.get("message"));
                }
            } catch (IllegalArgumentException e) {
                messages.add(e.getMessage());
            }
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("movedCount", movedCount);
        summary.put("messages", messages);
        return summary;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> moveBackToCart(Long userId, Long productId) {
        SaveForLater item = saveForLaterMapper.selectByUserIdAndProductId(userId, productId);
        if (item == null) {
            throw new IllegalArgumentException("稍后购买中无该商品");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品已不存在，无法移回购物车");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new IllegalArgumentException("商品「" + product.getName() + "」已下架，无法移回购物车");
        }

        int availableStock = product.getStock() != null ? product.getStock() : 0;
        int finalQuantity = item.getQuantity();
        String message = null;

        if (availableStock <= 0) {
            throw new IllegalArgumentException("商品「" + product.getName() + "」库存为零，无法移回购物车");
        }
        if (availableStock < item.getQuantity()) {
            finalQuantity = availableStock;
            message = "商品「" + product.getName() + "」库存仅剩 " + availableStock + " 件，已自动调整数量";
        }

        CartItem existingCart = cartItemMapper.selectByUserIdAndProductId(userId, productId);
        if (existingCart != null) {
            int combinedQuantity = existingCart.getQuantity() + finalQuantity;
            int maxCombined = Math.min(combinedQuantity, availableStock);
            if (maxCombined < combinedQuantity) {
                message = "商品「" + product.getName() + "」库存仅剩 " + availableStock + " 件，已自动调整数量";
            }
            existingCart.setQuantity(maxCombined);
            existingCart.setPriceSnapshot(product.getPrice());
            cartItemMapper.updateById(existingCart);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartItem.setQuantity(finalQuantity);
            cartItem.setChecked(0);
            cartItem.setPriceSnapshot(product.getPrice());
            cartItemMapper.insert(cartItem);
        }

        saveForLaterMapper.deleteByUserIdAndProductId(userId, productId);
        log.info("Moved back to cart: userId={}, productId={}, quantity={}", userId, productId, finalQuantity);

        Map<String, Object> result = new HashMap<>();
        result.put("adjustedQuantity", finalQuantity);
        result.put("message", message);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(Long userId, List<Long> productIds) {
        saveForLaterMapper.deleteByUserIdAndProductIds(userId, productIds);
        log.info("Batch deleted from save-for-later: userId={}, count={}", userId, productIds.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long productId) {
        saveForLaterMapper.deleteByUserIdAndProductId(userId, productId);
    }

    public List<SaveForLaterVO> list(Long userId, String sortBy) {
        List<SaveForLater> items = saveForLaterMapper.selectByUserId(userId);
        List<SaveForLaterVO> result = new ArrayList<>();
        for (SaveForLater item : items) {
            Product p = productMapper.selectById(item.getProductId());
            if (p == null) continue;
            SaveForLaterVO vo = new SaveForLaterVO();
            vo.setId(item.getId());
            vo.setProductId(p.getId());
            vo.setProductName(p.getName());
            vo.setProductImage(p.getMainImage());
            vo.setPriceSnapshot(item.getPriceSnapshot());
            vo.setCurrentPrice(p.getPrice());
            vo.setStock(p.getStock());
            vo.setQuantity(item.getQuantity());
            vo.setCreatedAt(item.getCreatedAt());

            boolean offline = p.getStatus() == null || p.getStatus() != 1;
            vo.setOffline(offline);
            vo.setProductStatus(p.getStatus() != null ? p.getStatus() : 0);

            BigDecimal drop = item.getPriceSnapshot().subtract(p.getPrice());
            vo.setPriceDropped(drop.compareTo(BigDecimal.ZERO) > 0);
            vo.setPriceDrop(drop);

            PriceAlert alert = priceAlertMapper.selectByUserIdAndProductId(userId, p.getId());
            if (alert != null) {
                vo.setAlertId(alert.getId());
                vo.setAlertTargetPrice(alert.getTargetPrice());
            }

            result.add(vo);
        }

        if ("price".equalsIgnoreCase(sortBy)) {
            result.sort(Comparator.comparing(SaveForLaterVO::getCurrentPrice));
        } else {
            result.sort(Comparator.comparing(SaveForLaterVO::getCreatedAt).reversed());
        }

        return result;
    }

    public int countByUserId(Long userId) {
        return saveForLaterMapper.countByUserId(userId);
    }
}
