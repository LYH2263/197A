package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项实体
 */
@Data
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Integer checked;
    private BigDecimal priceSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
