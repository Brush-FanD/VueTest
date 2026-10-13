package com.hkd.exception;

/**
 * 未登录异常：需要登录才能执行的操作，当前会话没有登录用户。
 * 由全局异常处理器统一转换为 401 响应，前端据此跳转登录页。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}