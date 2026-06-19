package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ProductImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;
    private String imageUrl;
    private Integer sortOrder;
    private Integer isMain;
    private LocalDateTime createdAt;
}
