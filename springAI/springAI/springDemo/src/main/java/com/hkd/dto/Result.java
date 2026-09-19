package com.hkd.dto;

import lombok.Data;

@Data
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<T>();
        result.setCode(200);    // 200 = HTTP成功状态码
        result.setMsg("success");
        result.setData(data);    // 实际数据
        return result;
    }
    /**
     * 失败响应的静态工厂方法
     *
     * 用法：Result.error("AI服务不可用")
     * 返回：{ "code": 500, "message": "AI服务不可用", "data": null }
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<T>();
        result.setCode(500);        // 500 = 服务器错误状态码
        result.setMsg(message);     // 错误提示信息
        return result;
    }
}
