package com.shop.service;

import com.shop.dto.CartItemVO;
import com.shop.entity.CartItem;
import com.shop.entity.Product;
import com.shop.mapper.CartItemMapper;
import com.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, Long productId, int quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("库存不足");
        }
        CartItem exist = cartItemMapper.selectByUserIdAndProductId(userId, productId);
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            cartItemMapper.updateById(exist);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setChecked(1);
            cartItemMapper.insert(item);
        }
        log.debug("Cart add: userId={}, productId={}, quantity={}", userId, productId, quantity);
    }

    public List<CartItemVO> list(Long userId) {
        List<CartItem> items = cartItemMapper.selectByUserId(userId);
        List<CartItemVO> result = new ArrayList<>();
        for (CartItem item : items) {
            Product p = productMapper.selectById(item.getProductId());
            if (p == null) continue;
            CartItemVO vo = new CartItemVO();
            vo.setId(item.getId());
            vo.setProductId(p.getId());
            vo.setProductName(p.getName());
            vo.setProductImage(p.getMainImage());
            vo.setPrice(p.getPrice());
            vo.setStock(p.getStock());
            vo.setQuantity(item.getQuantity());
            vo.setChecked(item.getChecked());
            vo.setTotalAmount(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            result.add(vo);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long userId, Long productId, int quantity) {
        CartItem item = cartItemMapper.selectByUserIdAndProductId(userId, productId);
        if (item == null) {
            throw new IllegalArgumentException("购物车中无该商品");
        }
        Product p = productMapper.selectById(productId);
        if (p != null && p.getStock() < quantity) {
            throw new IllegalArgumentException("库存不足");
        }
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setChecked(Long userId, Long productId, int checked) {
        cartItemMapper.updateChecked(userId, productId, checked);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long productId) {
        cartItemMapper.deleteByUserIdAndProductId(userId, productId);
    }
}
