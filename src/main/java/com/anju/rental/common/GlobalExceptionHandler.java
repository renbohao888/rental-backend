package com.anju.rental.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 捕获所有运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        String msg = e.getMessage();
        Result<String> result = new Result<>();
        result.setData(null);

        // 防护：防止 msg 为 null 时触发空指针异常 (NullPointerException)
        if (msg == null) {
            log.error("系统运行异常", e);
            result.setCode(500);
            result.setMessage("系统运行异常");
            return result;
        }

        // Token 相关异常拦截（匹配成功即返回 401）
        if (msg.contains("请先登录") || msg.contains("Token失效") || msg.contains("Token为空") || msg.contains("过期")) {
            result.setCode(401);
            result.setMessage(msg);
            return result;
        }

        // 权限不足拦截
        if (msg.contains("权限不足")) {
            result.setCode(403);
            result.setMessage(msg);
            return result;
        }

        // 其他未知异常打印堆栈并返回 500
        log.error("业务处理异常：{}", msg, e);
        result.setCode(500);
        result.setMessage(msg);
        return result;
    }
}