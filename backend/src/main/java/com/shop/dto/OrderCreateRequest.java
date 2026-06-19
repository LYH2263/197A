package com.shop.dto;

import lombok.Data;

@Data
public class OrderCreateRequest {

    private Long shippingAddressId;

    private String receiverName;

    private String receiverPhone;

    private String province;

    private String city;

    private String district;

    private String detailAddress;

    private String receiverAddress;

    private Boolean saveToAddressBook;
}
