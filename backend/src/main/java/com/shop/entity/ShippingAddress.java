package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ShippingAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String tag;
    private Integer isDefault;
    private Integer isDisabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getFullAddress() {
        return province + city + district + detailAddress;
    }
}
