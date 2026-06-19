package com.shop.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminProductQueryDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private Long categoryId;
    private Integer status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
