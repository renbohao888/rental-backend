package com.anju.rental.interceptor;

import com.anju.rental.annotation.RequiresRoles;
import com.anju.rental.common.BizException;
import com.anju.rental.common.JwtUtils;
import com.anju.rental.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT 认证与角色鉴权拦截器。
 *
 * <p>拦截 {@code /api/**}（放行路径见 {@code WebConfig}），职责：</p>
 * <ol>
 *   <li>校验 {@code Authorization} 请求头中的 Token（兼容带/不带 {@code Bearer } 前缀）；</li>
 *   <li>校验 Token 是否已被登出加入黑名单；</li>
 *   <li>解析出 {@code userId} / {@code role} 并挂到 request 属性（{@code realUserId} / {@code userRole}），供 Controller 读取；</li>
 *   <li>根据 {@link RequiresRoles} 注解做角色鉴权。</li>
 * </ol>
 *
 * <p>失败时抛出 {@link BizException}（401 / 403），由 {@code GlobalExceptionHandler} 统一转换为
 * 「HTTP 200 + body.code」的响应体，与前端既有约定保持一致。</p>
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    /** Bearer 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 请求属性名：当前登录用户 ID */
    public static final String ATTR_USER_ID = "realUserId";
    /** 请求属性名：当前登录用户角色（0-管理员，1-房东，2-租客） */
    public static final String ATTR_USER_ROLE = "userRole";
    /** 请求属性名：规范化后的 Token（已剥离 Bearer 前缀），供登出等接口复用 */
    public static final String ATTR_TOKEN = "authToken";

    private final TokenBlacklistService tokenBlacklistService;

    public JwtInterceptor(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 跨域预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 非 Controller 方法（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 取出并规范化 Token（兼容 "Bearer xxx" 与裸 token 两种写法）
        String token = resolveToken(request);
        if (token == null) {
            throw BizException.unauthorized("无访问权限，请先登录！（Token为空）");
        }

        Claims claims;
        try {
            claims = JwtUtils.parseToken(token);
        } catch (Exception e) {
            log.debug("Token 解析失败：{}", e.getMessage());
            throw BizException.unauthorized("Token失效或已过期，请重新登录！");
        }

        // 登出后的 Token 进入黑名单，不再放行
        if (tokenBlacklistService.isBlacklisted(token)) {
            throw BizException.unauthorized("登录状态已失效，请重新登录！");
        }

        // 将用户 ID 与角色写入请求上下文
        Long userId = claims.get("userId", Long.class);
        Integer role = claims.get("role", Integer.class);
        request.setAttribute(ATTR_USER_ID, userId);
        request.setAttribute(ATTR_USER_ROLE, role);
        request.setAttribute(ATTR_TOKEN, token);

        // 读取方法或类上的 @RequiresRoles 注解
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresRoles requiresRoles = handlerMethod.getMethodAnnotation(RequiresRoles.class);
        if (requiresRoles == null) {
            requiresRoles = handlerMethod.getBeanType().getAnnotation(RequiresRoles.class);
        }

        // 接口没有加权限注解，直接放行
        if (requiresRoles == null) {
            return true;
        }

        // 角色校验：匹配到允许的角色就放行
        if (role != null) {
            for (int allowedRole : requiresRoles.value()) {
                if (role == allowedRole) {
                    return true;
                }
            }
        }

        // 角色不匹配，抛出权限不足异常
        log.debug("角色鉴权失败：当前角色={}，允许角色={}", role, requiresRoles.value());
        throw BizException.forbidden("权限不足，无权访问该接口");
    }

    /**
     * 从请求头解析 Token，自动剥离 {@code Bearer } 前缀。
     *
     * @return 规范化后的 Token；不存在或为空时返回 {@code null}
     */
    private String resolveToken(HttpServletRequest request) {
        String raw = request.getHeader("Authorization");
        if (raw == null) {
            return null;
        }
        String token = raw.trim();
        if (token.isEmpty()) {
            return null;
        }
        if (token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return token.isEmpty() ? null : token;
    }
}
