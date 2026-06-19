package com.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户展示 DTO（不含密码）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private String role;
    private LocalDateTime createdAt;
}
