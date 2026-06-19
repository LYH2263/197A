package com.shop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 评价请求 DTO
 */
@Data
public class ReviewRequest {

    private Long parentId;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "评分不能为空")
    @Min(1)
    @Max(5)
    private Integer rating;

    private String content;

    private List<String> images;
}
