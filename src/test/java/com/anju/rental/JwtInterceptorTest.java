package com.anju.rental;

import com.anju.rental.annotation.RequiresRoles;
import com.anju.rental.common.BizException;
import com.anju.rental.common.JwtUtils;
import com.anju.rental.interceptor.JwtInterceptor;
import com.anju.rental.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JWT 拦截器单元测试：覆盖未登录、Token 非法、黑名单、角色鉴权与放行等分支。
 */
@ExtendWith(MockitoExtension.class)
class JwtInterceptorTest {

    private static final String SECRET = "interceptor-test-secret-key-2025-abcdef";

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private JwtInterceptor interceptor;

    /** 测试用控制器：模拟带 @RequiresRoles 与不带的接口 */
    static class DummyController {
        public void openApi() {
        }

        @RequiresRoles({0})
        public void adminOnly() {
        }
    }

    @BeforeEach
    void setUp() {
        JwtUtils.setConfig(SECRET, 3600000L);
        interceptor = new JwtInterceptor(tokenBlacklistService);
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        Method method = DummyController.class.getMethod(methodName);
        return new HandlerMethod(new DummyController(), method);
    }

    @Test
    @DisplayName("无 Token：抛出 401")
    void testMissingToken() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        BizException e = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, response, handler("openApi")));
        assertEquals(401, e.getCode());
    }

    @Test
    @DisplayName("空字符串 Token：抛出 401")
    void testBlankToken() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("   ");

        BizException e = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, response, handler("openApi")));
        assertEquals(401, e.getCode());
    }

    @Test
    @DisplayName("非法 Token：抛出 401")
    void testInvalidToken() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("not-a-valid-jwt");

        BizException e = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, response, handler("openApi")));
        assertEquals(401, e.getCode());
    }

    @Test
    @DisplayName("已登出（命中黑名单）：抛出 401")
    void testBlacklistedToken() throws Exception {
        String token = JwtUtils.generateToken(7L, 2);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        BizException e = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, response, handler("openApi")));
        assertEquals(401, e.getCode());
    }

    @Test
    @DisplayName("有效 Token + 无角色注解：放行并挂载用户身份")
    void testValidToken() throws Exception {
        String token = JwtUtils.generateToken(9L, 1);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(token);

        assertTrue(interceptor.preHandle(request, response, handler("openApi")));
        verify(request).setAttribute("realUserId", 9L);
        verify(request).setAttribute("userRole", 1);
        verify(request).setAttribute("authToken", token);
    }

    @Test
    @DisplayName("兼容 Bearer 前缀")
    void testBearerPrefix() throws Exception {
        String token = JwtUtils.generateToken(11L, 2);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertTrue(interceptor.preHandle(request, response, handler("openApi")));
        verify(request).setAttribute("authToken", token);
    }

    @Test
    @DisplayName("角色匹配：管理员访问管理员接口放行")
    void testRoleMatched() throws Exception {
        String token = JwtUtils.generateToken(1L, 0);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn(token);

        assertTrue(interceptor.preHandle(request, response, handler("adminOnly")));
    }

    @Test
    @DisplayName("角色不匹配：租客访问管理员接口抛出 403")
    void testRoleForbidden() throws Exception {
        String token = JwtUtils.generateToken(2L, 2);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn(token);

        BizException e = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, response, handler("adminOnly")));
        assertEquals(403, e.getCode());
    }

    @Test
    @DisplayName("OPTIONS 预检请求直接放行")
    void testOptionsPreflight() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");

        assertTrue(interceptor.preHandle(request, response, handler("adminOnly")));
    }

    @Test
    @DisplayName("非 Controller 方法（静态资源等）直接放行")
    void testNonHandlerMethod() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }
}
