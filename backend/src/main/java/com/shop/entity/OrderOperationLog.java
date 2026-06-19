package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrderOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String operation;
    private Integer oldStatus;
    private Integer newStatus;
    private String remark;
    private String extraInfo;
    private LocalDateTime createdAt;
}
