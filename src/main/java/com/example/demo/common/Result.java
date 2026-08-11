package com.example.demo.common;

import lombok.Data;

/**
 * 全局统一返回结果类
 * @param <T> 返回数据的泛型
 */
@Data // 自动生成get、set、toString、equals、hashCode
public class Result<T> {

    // 1. 状态码：200成功，500失败
    private Integer code;
    // 2. 提示信息
    private String message;
    // 3. 泛型核心数据
    private T data;

    // ========== 静态工具方法（简化调用，可选但推荐写）==========
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