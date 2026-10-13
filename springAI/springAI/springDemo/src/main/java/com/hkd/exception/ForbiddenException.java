package com.hkd.exception;

/**
 * 越权异常：请求本身合法，但当前会话不是这条资源的拥有者。
 * 由全局异常处理器统一转换为 403 响应。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}