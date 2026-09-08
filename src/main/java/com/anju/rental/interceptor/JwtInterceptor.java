package com.anju.rental.interceptor;

import com.anju.rental.annotation.RequiresRoles;
import com.anju.rental.common.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行OPTIONS跨域预检请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }


        // 非Controller方法（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 从 Header 获取 Token
        String token = request.getHeader("Authorization");
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("无访问权限，请先登录！（Token为空）");
        }

        Claims claims;
        try {
            // 解析 Token
            claims = JwtUtils.parseToken(token);
        } catch (Exception e) {
            throw new RuntimeException("Token失效或已过期，请重新登录！");
        }

        // 将用户 ID 与角色写入请求上下文
        Long userId = claims.get("userId", Long.class);
        Integer role = claims.get("role", Integer.class);
        request.setAttribute("realUserId", userId);
        request.setAttribute("userRole", role);

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
        int[] allowedRoles = requiresRoles.value();
        for (int allowedRole : allowedRoles) {
            if (role != null && role == allowedRole) {
                return true;
            }
        }

        // 角色不匹配，抛出权限不足异常
        throw new RuntimeException("权限不足，无权访问该接口");
    }
}