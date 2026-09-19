package com.hkd.exception;

/**
 * AI 服务异常：模型接口超时、限流、上游报错等。
 * 由全局异常处理器统一转换为 503 响应，避免把上游原始报错（可能含密钥信息）返回给前端。
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
