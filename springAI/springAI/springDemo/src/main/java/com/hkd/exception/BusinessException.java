package com.hkd.exception;

/**
 * 业务异常：参数不合法、资源不存在等可预期的错误。
 * 由全局异常处理器统一转换为 400 响应，前端只需要展示 message。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
