package com.anju.rental;

import com.anju.rental.common.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务异常单元测试
 */
class BizExceptionTest {

    @Test
    @DisplayName("默认构造：错误码为 500")
    void testDefaultCode() {
        BizException e = new BizException("出错了");
        assertEquals(BizException.CODE_SERVER_ERROR, e.getCode());
        assertEquals("出错了", e.getMessage());
    }

    @Test
    @DisplayName("指定错误码构造")
    void testExplicitCode() {
        BizException e = new BizException(418, "我是茶壶");
        assertEquals(418, e.getCode());
        assertEquals("我是茶壶", e.getMessage());
    }

    @Test
    @DisplayName("静态工厂方法对应正确的错误码")
    void testFactoryMethods() {
        assertEquals(BizException.CODE_BAD_REQUEST, BizException.badRequest("参数错误").getCode());
        assertEquals(BizException.CODE_UNAUTHORIZED, BizException.unauthorized("未登录").getCode());
        assertEquals(BizException.CODE_FORBIDDEN, BizException.forbidden("无权限").getCode());
        assertEquals(BizException.CODE_NOT_FOUND, BizException.notFound("不存在").getCode());
    }

    @Test
    @DisplayName("BizException 是 RuntimeException 的子类（可被全局异常处理器捕获）")
    void testIsRuntimeException() {
        assertInstanceOf(RuntimeException.class, new BizException("x"));
    }

    @Test
    @DisplayName("支持携带原始异常，便于排查根因")
    void testCausePreserved() {
        IllegalStateException cause = new IllegalStateException("root");
        BizException e = new BizException(BizException.CODE_SERVER_ERROR, "包装后", cause);
        assertSame(cause, e.getCause());
    }
}
