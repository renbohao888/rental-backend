package com.example.demo.interceptor;

import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.JwtUtils;
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
        // 1. 放行OPTIONS跨域预检请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }


        // 2. 非Controller方法（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 3. 从Header获取Token，和你项目的传参方式一致（无需加Bearer）
        String token = request.getHeader("Authorization");
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("无访问权限，请先登录！（Token为空）");
        }

        Claims claims;
        try {
            // 4. 调用项目内置的JwtUtils解析Token，密钥已自动处理
            claims = JwtUtils.parseToken(token);
        } catch (Exception e) {
            throw new RuntimeException("Token失效或已过期，请重新登录！");
        }

        // 5. 把用户ID、角色存入请求上下文，Controller里直接取不会再空指针
        Long userId = claims.get("userId", Long.class);
        Integer role = claims.get("role", Integer.class);
        request.setAttribute("realUserId", userId);
        request.setAttribute("userRole", role);

        // 6. 读取接口的@RequiresRoles权限注解（优先读方法上的，其次读类上的）
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresRoles requiresRoles = handlerMethod.getMethodAnnotation(RequiresRoles.class);
        if (requiresRoles == null) {
            requiresRoles = handlerMethod.getBeanType().getAnnotation(RequiresRoles.class);
        }

        // 7. 接口没有加权限注解，直接放行
        if (requiresRoles == null) {
            return true;
        }

        // 8. 角色校验：匹配到允许的角色就放行
        int[] allowedRoles = requiresRoles.value();
        for (int allowedRole : allowedRoles) {
            if (role != null && role == allowedRole) {
                return true;
            }
        }

        // 9. 角色不匹配，抛出权限不足异常
        throw new RuntimeException("权限不足，无权访问该接口");
    }
}