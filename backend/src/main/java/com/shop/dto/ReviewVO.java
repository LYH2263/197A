package com.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价展示 DTO（含商品名、用户名）
 */
@Data
public class ReviewVO {

    private Long id;
    private Long parentId;
    private Integer reviewType;
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private Long orderId;
    private Integer rating;
    private String content;
    private List<String> images;
    private String replyContent;
    private LocalDateTime replyAt;
    private LocalDateTime createdAt;
    private Boolean canFollowup;
    private String followupDisabledReason;
    private Boolean canEdit;
    private List<ReviewVO> followups;
}
