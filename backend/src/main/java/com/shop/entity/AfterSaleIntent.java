package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AfterSaleIntent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String intentSource;
    private LocalDateTime triggeredAt;
    private Integer handled;
    private LocalDateTime handledAt;
    private String remark;
}
