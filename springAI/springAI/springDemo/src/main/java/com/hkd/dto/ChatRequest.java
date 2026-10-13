package com.hkd.dto;

import lombok.Data;

@Data
public class ChatRequest {

    /** 用户提问；会话身份由服务端 Cookie 决定，不需要前端传 */
    private String message;
}