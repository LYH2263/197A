package com.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private int successCount;
    private int failCount;
    private List<String> failReasons;
}
