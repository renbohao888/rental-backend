package com.anju.rental.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器。
 *
 * <p>统一把各类异常转换为 {@link Result} 返回，保持「HTTP 200 + body.code」的既有契约，
 * 前端无需改动即可继续按 {@code data.code} 判定结果。</p>
 *
 * <p>错误码来源（按优先级）：</p>
 * <ol>
 *   <li>{@link BizException} 自带的业务错误码（认证 401 / 鉴权 403 / 参数 400 等）；</li>
 *   <li>参数校验类异常统一为 400，并只回显第一条校验失败信息，避免刷屏；</li>
 *   <li>其余未预期异常统一为 500，仅记录日志、不向前端泄露堆栈细节。</li>
 * </ol>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常：直接使用异常自带的错误码。
     */
    @ExceptionHandler(BizException.class)
    public Result<String> handleBizException(BizException e) {
        log.debug("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return build(e.getCode(), e.getMessage());
    }

    /**
     * @RequestBody + @Valid 校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = firstFieldError(e.getBindingResult().getFieldError());
        log.debug("参数校验失败：{}", message);
        return build(BizException.CODE_BAD_REQUEST, message);
    }

    /**
     * 表单对象绑定校验失败。
     */
    @ExceptionHandler(BindException.class)
    public Result<String> handleBindException(BindException e) {
        String message = firstFieldError(e.getBindingResult().getFieldError());
        log.debug("参数绑定失败：{}", message);
        return build(BizException.CODE_BAD_REQUEST, message);
    }

    /**
     * 方法参数（@RequestParam / @PathVariable）约束校验失败。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("参数校验失败");
        log.debug("参数约束校验失败：{}", message);
        return build(BizException.CODE_BAD_REQUEST, message);
    }

    /**
     * 请求体无法解析（JSON 语法错误、字段类型不匹配等）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.debug("请求体解析失败：{}", e.getMessage());
        return build(BizException.CODE_BAD_REQUEST, "请求体格式不正确，请检查提交的数据");
    }

    /**
     * 缺少必填的请求参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<String> handleMissingParam(MissingServletRequestParameterException e) {
        return build(BizException.CODE_BAD_REQUEST, "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 请求参数类型不匹配（例如路径变量应为数字却传了字符串）。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return build(BizException.CODE_BAD_REQUEST, "参数类型不正确：" + e.getName());
    }

    /**
     * 上传文件超出大小限制。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件超出大小限制：{}", e.getMessage());
        return build(BizException.CODE_BAD_REQUEST, "上传文件过大，请压缩后重试");
    }

    /**
     * 静态资源 / 未知路径 404。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<String> handleNoResourceFound(NoResourceFoundException e) {
        return build(BizException.CODE_NOT_FOUND, "请求的资源不存在");
    }

    /**
     * 兜底：捕获所有未预期的运行时异常。
     * <p>日志记录完整堆栈，响应中只给出通用提示，避免向前端泄露服务端内部信息。</p>
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        log.error("系统运行异常", e);
        String msg = e.getMessage();
        return build(BizException.CODE_SERVER_ERROR,
                (msg == null || msg.isBlank()) ? "系统运行异常" : msg);
    }

    /**
     * 兜底：非运行时异常。
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return build(BizException.CODE_SERVER_ERROR, "系统异常，请稍后再试");
    }

    /* ---------------- 私有工具方法 ---------------- */

    private Result<String> build(int code, String message) {
        Result<String> result = new Result<>();
        result.setCode(code);
        result.setMessage(message == null ? "操作失败" : message);
        result.setData(null);
        return result;
    }

    private String firstFieldError(FieldError fieldError) {
        if (fieldError == null) {
            return "参数校验失败";
        }
        String defaultMessage = fieldError.getDefaultMessage();
        return (defaultMessage == null || defaultMessage.isBlank())
                ? fieldError.getField() + " 参数不合法"
                : defaultMessage;
    }
}
