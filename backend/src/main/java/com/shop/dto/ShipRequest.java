package com.shop.dto;

import lombok.Data;

@Data
public class ShipRequest {
    private Long orderId;
    private String logisticsCompany;
    private String trackingNo;
    private String shippingRemark;
}
