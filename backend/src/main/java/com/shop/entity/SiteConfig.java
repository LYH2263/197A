package com.shop.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteConfig {

    private Long id;
    private String configKey;
    private String configValue;
    private LocalDateTime updatedAt;
}
