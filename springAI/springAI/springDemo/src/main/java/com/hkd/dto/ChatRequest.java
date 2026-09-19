package com.hkd.dto;

import lombok.Data;

@Data
public class ChatRequest {

    private String message;

    private String conversationId="default";
}
