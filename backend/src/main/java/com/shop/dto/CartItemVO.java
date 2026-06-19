package com.shop.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项展示 DTO（含商品信息）
 */
@Data
public class CartItemVO {

    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer stock;
    private Integer quantity;
    private Integer checked;
    private BigDecimal totalAmount;
}
