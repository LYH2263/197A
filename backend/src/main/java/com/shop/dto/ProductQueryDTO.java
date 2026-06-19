package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long categoryId;

    private String keyword;

    private Integer status;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String stockStatus;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String sortBy;

    private String sortOrder;
}
