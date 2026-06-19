package com.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportErrorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int rowNum;
    private String field;
    private String message;
}
