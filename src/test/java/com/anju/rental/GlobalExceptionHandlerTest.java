package com.anju.rental;

import com.anju.rental.common.BizException;
import com.anju.rental.common.GlobalExceptionHandler;
import com.anju.rental.common.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理器单元测试（不启动 Spring 容器，直接调用处理方法）。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("业务异常：直接使用异常自带的错误码与提示")
    void testBizException() {
        Result<String> result = handler.handleBizException(BizException.unauthorized("Token失效或已过期，请重新登录！"));

        assertEquals(401, result.getCode());
        assertEquals("Token失效或已过期，请重新登录！", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("业务异常：403 鉴权失败")
    void testForbidden() {
        Result<String> result = handler.handleBizException(BizException.forbidden("权限不足，无权访问该接口"));

        assertEquals(403, result.getCode());
        assertEquals("权限不足，无权访问该接口", result.getMessage());
    }

    @Test
    @DisplayName("参数绑定失败：取第一条字段校验信息并返回 400")
    void testBindException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "phoneForm");
        bindingResult.addError(new FieldError("phoneForm", "phone", "手机号格式不正确"));

        Result<String> result = handler.handleBindException(new BindException(bindingResult));

        assertEquals(400, result.getCode());
        assertEquals("手机号格式不正确", result.getMessage());
    }

    @Test
    @DisplayName("参数绑定失败：没有默认提示时回退为字段名提示")
    void testBindExceptionWithoutDefaultMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "roomForm");
        bindingResult.addError(new FieldError("roomForm", "price", null));

        Result<String> result = handler.handleBindException(new BindException(bindingResult));

        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("price"));
    }

    @Test
    @DisplayName("未预期运行时异常：返回 500 并保留原始提示")
    void testRuntimeException() {
        Result<String> result = handler.handleRuntimeException(new RuntimeException("房源不存在"));

        assertEquals(500, result.getCode());
        assertEquals("房源不存在", result.getMessage());
    }

    @Test
    @DisplayName("未预期运行时异常：message 为空时给出通用提示，避免返回 null")
    void testRuntimeExceptionWithNullMessage() {
        Result<String> result = handler.handleRuntimeException(new RuntimeException());

        assertEquals(500, result.getCode());
        assertEquals("系统运行异常", result.getMessage());
    }

    @Test
    @DisplayName("兜底异常：非运行时异常返回通用提示，不泄露内部信息")
    void testCheckedException() {
        Result<String> result = handler.handleException(new Exception("内部数据库细节"));

        assertEquals(500, result.getCode());
        assertEquals("系统异常，请稍后再试", result.getMessage());
    }
}
