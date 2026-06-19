package com.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价展示 DTO（含商品名、用户名）
 */
@Data
public class ReviewVO {

    private Long id;
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private Long orderId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
