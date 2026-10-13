package com.hkd.dto;

import lombok.Data;

/** 登录请求体 */
@Data
public class LoginRequest {

    private String username;

    private String password;
}