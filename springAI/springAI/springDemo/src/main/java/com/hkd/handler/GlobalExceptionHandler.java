package com.hkd.handler;

import com.hkd.dto.Result;
import com.hkd.exception.AiServiceException;
import com.hkd.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 全局异常处理器：所有接口的错误响应都收敛成 Result 结构，避免把堆栈和内部信息抛给前端。
 *
 * <p>继承 ResponseEntityExceptionHandler 是为了让 Spring MVC 自带的异常
 * （404、405、参数缺失、上传文件过大等）继续由框架的专用方法处理，
 * 不会被下面的兜底 handler 误吞成 500。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** 业务校验失败：参数不合法、资源不存在 */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务校验失败：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /** AI 服务不可用：超时、限流、上游报错 */
    @ExceptionHandler(AiServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleAiService(AiServiceException e) {
        log.error("AI 服务调用失败", e);
        return Result.error(e.getMessage());
    }

    /** 兜底：未预期的异常只记录日志，响应里不暴露任何内部细节 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnexpected(Exception e) {
        log.error("服务器内部错误", e);
        return Result.error("服务器开小差了，请稍后重试");
    }

    /** 统一 Spring MVC 内置异常的响应体格式 */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String message;
        if (status == HttpStatus.PAYLOAD_TOO_LARGE) {
            message = "文件太大，请压缩后再上传";
        } else if (status != null && status.is4xxClientError()) {
            message = "请求地址或参数有误";
        } else {
            message = "服务器开小差了，请稍后重试";
        }
        log.warn("请求处理失败：{} {}", statusCode, ex.getMessage());
        return super.handleExceptionInternal(ex, Result.error(message), headers, statusCode, request);
    }
}
