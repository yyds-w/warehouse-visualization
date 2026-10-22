package com.warehouse.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;
    private String message;
    private Boolean success;
    private T data;
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok() {
        return ok("操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return ok("操作成功", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setSuccess(true);
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(400, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }
}
