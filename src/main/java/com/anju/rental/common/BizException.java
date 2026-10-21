package com.anju.rental.common;

/**
 * 业务异常：携带明确的业务错误码，供全局异常处理器直接映射为统一返回结构。
 *
 * <p>相比直接用 {@code new RuntimeException("提示语")}，本异常把「错误码」与「提示语」解耦：
 * 全局异常处理器不再依赖对提示语做字符串匹配来猜测错误码，语义更清晰、更不容易出错。</p>
 *
 * <p>约定错误码（与 {@link Result#getCode()}、HTTP 语义保持一致）：</p>
 * <ul>
 *   <li>400 参数校验 / 请求不合法</li>
 *   <li>401 未登录 / Token 无效或过期</li>
 *   <li>403 已登录但角色权限不足</li>
 *   <li>404 资源不存在</li>
 *   <li>500 服务端处理失败</li>
 * </ul>
 *
 * <p>注意：为兼容前端已有的「HTTP 200 + body.code」判定方式，本异常默认不改变 HTTP 状态码，
 * 由 {@code GlobalExceptionHandler} 统一包装为 {@link Result} 返回。</p>
 */
public class BizException extends RuntimeException {

    /** 默认业务错误码：服务端处理失败 */
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_SERVER_ERROR = 500;

    /** 业务错误码 */
    private final int code;

    public BizException(String message) {
        this(CODE_SERVER_ERROR, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /* ---------------- 常用快捷构造 ---------------- */

    public static BizException badRequest(String message) {
        return new BizException(CODE_BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(CODE_UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(CODE_FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(CODE_NOT_FOUND, message);
    }
}
