package com.shop.service;

import com.shop.entity.Notification;
import com.shop.entity.PriceAlert;
import com.shop.entity.Product;
import com.shop.mapper.NotificationMapper;
import com.shop.mapper.PriceAlertMapper;
import com.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertService.class);

    private final PriceAlertMapper priceAlertMapper;
    private final ProductMapper productMapper;
    private final NotificationMapper notificationMapper;

    @Transactional(rollbackFor = Exception.class)
    public void subscribe(Long userId, Long productId, BigDecimal targetPrice) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("目标价格必须大于0");
        }

        PriceAlert alert = new PriceAlert();
        alert.setUserId(userId);
        alert.setProductId(productId);
        alert.setTargetPrice(targetPrice);
        alert.setNotified(0);
        priceAlertMapper.insert(alert);
        log.info("Price alert subscribed: userId={}, productId={}, targetPrice={}", userId, productId, targetPrice);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unsubscribe(Long userId, Long productId) {
        priceAlertMapper.deleteByUserIdAndProductId(userId, productId);
    }

    public List<PriceAlert> listByUserId(Long userId) {
        return priceAlertMapper.selectByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkAndNotify(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) return;

        List<PriceAlert> alerts = priceAlertMapper.selectUnnotifiedByProductId(productId, product.getPrice());
        for (PriceAlert alert : alerts) {
            Notification notification = new Notification();
            notification.setUserId(alert.getUserId());
            notification.setType("PRICE_DROP");
            notification.setContent("您关注的商品「" + product.getName() + "」已降至 ¥" + product.getPrice() + "，达到您的目标价 ¥" + alert.getTargetPrice());
            notification.setIsRead(0);
            notificationMapper.insert(notification);

            priceAlertMapper.updateNotified(alert.getId(), 1);
            log.info("Price alert triggered: userId={}, productId={}, targetPrice={}, currentPrice={}",
                    alert.getUserId(), productId, alert.getTargetPrice(), product.getPrice());
        }
    }
}
