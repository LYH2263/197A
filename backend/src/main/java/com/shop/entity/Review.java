package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品评价实体
 */
@Data
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int TYPE_INITIAL = 0;
    public static final int TYPE_FOLLOWUP = 1;

    private Long id;
    private Long parentId;
    private Integer reviewType;
    private Long userId;
    private Long productId;
    private Long orderId;
    private Integer rating;
    private String content;
    private String images;
    private String replyContent;
    private LocalDateTime replyAt;
    private LocalDateTime createdAt;
}
