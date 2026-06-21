package com.health.diet.common;

/* 统一响应封装 */
public class ApiResponse<T> {

    /* 状态码 */
    private int code;
    /* 消息 */
    private String message;
    /* 数据 */
    private T data;

    private ApiResponse() {}

    /* 成功响应（带数据） */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    /* 成功响应（无数据） */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /* 失败响应 */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
