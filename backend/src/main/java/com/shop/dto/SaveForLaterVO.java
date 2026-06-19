package com.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SaveForLaterVO {

    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal priceSnapshot;
    private BigDecimal currentPrice;
    private BigDecimal priceDrop;
    private Boolean priceDropped;
    private Integer stock;
    private Integer quantity;
    private Long alertId;
    private BigDecimal alertTargetPrice;
    private LocalDateTime createdAt;
}
