package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BatchOperationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> productIds;
    private Integer status;
    private Long categoryId;
    private BigDecimal priceRatio;
}
