package com.shop.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
