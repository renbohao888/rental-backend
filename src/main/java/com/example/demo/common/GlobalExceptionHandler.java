package com.example.demo.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获所有运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        String msg = e.getMessage();
        Result<String> result = new Result<>();
        result.setData(null);

        // 1. 防护：防止 msg 为 null 时触发空指针异常 (NullPointerException)
        if (msg == null) {
            e.printStackTrace();
            result.setCode(500);
            result.setMessage("系统运行异常");
            return result;
        }

        // 2. Token 相关异常拦截（匹配成功即返回 401）
        if (msg.contains("请先登录") || msg.contains("Token失效") || msg.contains("Token为空") || msg.contains("过期")) {
            result.setCode(401);
            result.setMessage(msg);
            return result;
        }

        // 3. 权限不足拦截
        if (msg.contains("权限不足")) {
            result.setCode(403);
            result.setMessage(msg);
            return result;
        }

        // 4. 其他未知异常打印堆栈并返回 500
        e.printStackTrace();
        result.setCode(500);
        result.setMessage(msg);
        return result;
    }
}