package com.anju.rental.config;

import com.anju.rental.annotation.RequiresRoles;
import com.anju.rental.common.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证与角色鉴权拦截器
 * <p>
 * 拦截所有 /api/** 请求，校验 Token 有效性并根据 @RequiresRoles 注解进行角色权限校验。
 * </p>
 */
@Component
public class WebConfigJwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebConfigJwtInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 非控制器方法（如静态资源、Swagger 文档等）直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        log.debug("JWT 拦截器：开始校验请求 [{} {}]", request.getMethod(), request.getRequestURI());

        // 从请求头中获取 Authorization Token
        String token = request.getHeader("Authorization");

        // 无 Token 则返回 401
        if (token == null || token.trim().isEmpty()) {
            log.warn("JWT 拦截器：缺少 Authorization 请求头，返回 401");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录，请先登录获取Token");
            return false;
        }

        // 验证 Token 真伪并检查角色
        try {
            // 自动剥离 Bearer 前缀
            String jwt = token;
            if (token.startsWith("Bearer ")) {
                jwt = token.substring(7);
            }

            // 解析 Token，提取用户身份信息
            Claims claims = JwtUtils.parseToken(jwt);
            Long currentUserId = claims.get("userId", Long.class);
            Integer userRole = claims.get("role", Integer.class); // 0-管理员, 1-房东, 2-租客

            // 将真实用户 ID 挂载到 request 属性中，供 Controller 使用
            request.setAttribute("realUserId", currentUserId);
            request.setAttribute("userRole", userRole);

            // 检查目标方法/类上的 @RequiresRoles 注解
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequiresRoles requiresRoles = handlerMethod.getMethodAnnotation(RequiresRoles.class);
            if (requiresRoles == null) {
                requiresRoles = handlerMethod.getBeanType().getAnnotation(RequiresRoles.class);
            }

            // 如果有角色限制，进行角色匹配
            if (requiresRoles != null) {
                int[] allowedRoles = requiresRoles.value();
                boolean isMatch = false;
                for (int role : allowedRoles) {
                    if (role == userRole) {
                        isMatch = true;
                        break;
                    }
                }

                if (!isMatch) {
                    log.warn("JWT 拦截器：角色不匹配，当前角色={}，允许角色={}，返回 403", userRole, allowedRoles);
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "您的账号角色权限不足，拒绝访问！");
                    return false;
                }
            }

            log.debug("JWT 拦截器：Token 有效，角色匹配，放行");
            return true;

        } catch (Exception e) {
            // Token 无效、篡改或已过期
            log.warn("JWT 拦截器：Token 解析失败，返回 401 - {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token无效或已过期");
            return false;
        }
    }

    /**
     * 统一的 JSON 异常响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + msg + "\",\"data\":null}");
    }
}