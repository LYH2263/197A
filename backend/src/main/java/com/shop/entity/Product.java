package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long categoryId;
    private String name;
    private String subtitle;
    private String mainImage;
    private String detail;
    private BigDecimal price;
    private Integer stock;
    private Integer salesCount;
    private Integer reviewCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ProductImage> images;
}
