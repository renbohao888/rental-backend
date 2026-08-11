package com.example.demo.config;

import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.JwtUtils; // 👈 完美引入你上一轮给我的真实工具类
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WebConfigJwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 0. 如果请求的不是控制器方法（例如静态资源、Swagger等），保安直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        System.out.println("====== 保安2.0：开始检查权限 ======");

        // 1. 从请求头中获取 Token
        String token = request.getHeader("Authorization");

        // 2. 如果没有任何 Token，直接拦下，返回 401 无权限
        if (token == null || token.trim().isEmpty()) {
            System.out.println("保安：没戴手环，禁止入内！");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录，请先登录获取Token");
            return false;
        }

        // 3. 开始验证 Token 真伪并检查角色
        try {
            // 如果前端传的值带有标准的 Bearer 前缀，自动剥离
            String jwt = token;
            if (token.startsWith("Bearer ")) {
                jwt = token.substring(7);
            }

            // 🔓 调用你写好的工具类解析真正的 Token
            Claims claims = JwtUtils.parseToken(jwt);

            // 提取当初存进去的 userId 和 role
            Long currentUserId = claims.get("userId", Long.class);
            Integer userRole = claims.get("role", Integer.class); // 0-管理员, 1-房东, 2-租客

            // 保安查明身份后，把真实 ID 挂在 request 对象上，带进 Controller
            request.setAttribute("realUserId", currentUserId);

            // 4. 【核心升级】：获取目标方法/类上是否标有角色权限注解
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequiresRoles requiresRoles = handlerMethod.getMethodAnnotation(RequiresRoles.class);
            if (requiresRoles == null) {
                // 如果方法上没写，再看看类（Controller类）上有没有写
                requiresRoles = handlerMethod.getBeanType().getAnnotation(RequiresRoles.class);
            }

            // 5. 如果加了角色限制注解，开始比对角色
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
                    System.out.println("保安：手环是真的，但你不是VIP角色（当前角色" + userRole + "不在允许范围内），拦下！");
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "您的账号角色权限不足，拒绝访问！");
                    return false;
                }
            }

            System.out.println("保安：手环有效，角色匹配，放行！");
            return true; // 放行进入 Controller

        } catch (Exception e) {
            // 如果 Token 伪造、篡改或已过期，解析会报错，保安在此拦下
            System.out.println("保安：假手环或手环已过期！抓起来！");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token无效或已过期");
            return false;
        }
    }

    /**
     * 抽取统一的 JSON 异常返回工具方法
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + msg + "\"}");
    }
}