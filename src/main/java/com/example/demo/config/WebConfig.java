package com.example.demo.config;

import com.example.demo.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    /**
     * 全局跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 1. 用户认证相关（未登录时必须可访问）
                        "/api/user/login",
                        "/api/user/loginByPhone",
                        "/api/user/register",
                        "/api/user/sendSms",
                        "/api/user/smsLogin",
                        "/api/user/registerByPhone",
                        "/api/user/resetPassword",
                        "/api/user/detail/**",        // 公开用户信息（房源详情页展示房东等）

                        // 2. 房源相关公共查询（游客、未登录用户可浏览）
                        // 注意：不能排除 /api/room/my、/api/room/status/**, /api/room/add 等带鉴权的接口
                        "/api/room/list",
                        "/api/room/detail/**",         // 房源详情（游客可看）
                        "/api/room/recommend/hot",
                        "/api/room/calendar/**",       // 房态日历（游客可看）

                        // 2.1 AI 租赁助手（游客可直接咨询房源）
                        "/api/ai/**",

                        // 3. 支付回调（第三方服务器调用，绝对不能带Token拦截）
                        "/api/pay/notify",
                        "/api/pay/aliPay",

                        // 4. 公告与轮播图、平台配置等公共信息
                        // 注意：管理员公告管理（/api/notice/admin/**、/api/notice/add、/api/notice/update、/api/notice/admin/delete/**）
                        // 必须经过鉴权，不能整体排除，否则会因缺少 userRole 而报"无权限"
                        // 公告详情统一放行 /api/notice/detail/**，不能排除 /api/notice/{noticeId}，
                        // 因为该 Ant 模式会同时匹配 /add、/update，导致管理员接口绕过 JWT 鉴权
                        "/api/notice/list",
                        "/api/notice/detail/**",       // 公告详情（游客可看）
                        "/api/evaluation/room/**",     // 放行评价列表查看
                        "/api/banner/list",
                        // 仅放行公开的平台配置（名称、电话）；/api/config/admin/** 必须经过鉴权
                        "/api/config/platform/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /uploads/** 到配置的上传目录（绝对路径，避免因工作目录不同导致找不到路径）
        String location = uploadPath.endsWith("/") || uploadPath.endsWith("\\") ? uploadPath : uploadPath + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }
}