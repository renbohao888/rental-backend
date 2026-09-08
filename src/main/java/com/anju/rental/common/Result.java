package com.anju.rental.common;

import lombok.Data;

/**
 * 全局统一返回结果类
 * @param <T> 返回数据的泛型
 */
@Data
public class Result<T> {

    // 状态码：200成功，500失败
    private Integer code;
    // 提示信息
    private String message;
    // 数据
    private T data;

    // 静态工厂方法
    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功返回（自定义提示信息）
     */
    public static <T> Result<T> success(T data, String msg) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(msg);
        result.setData(data);
        return result;
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(msg);
        result.setData(null);
        return result;
    }
}