package com.hkd.dto;

import lombok.Data;

/** 注册请求体 */
@Data
public class RegisterRequest {

    private String username;

    private String password;

    /** 可选，留空时默认用用户名 */
    private String displayName;
}