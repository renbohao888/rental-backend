package com.anju.rental.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI (Swagger) 配置
 * <p>
 * 访问地址：<a href="http://localhost:8080/swagger-ui.html">Swagger UI</a><br>
 * API 文档 JSON：<a href="http://localhost:8080/api-docs">/api-docs</a>
 * </p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("短期房屋租赁管理系统 API")
                        .version("1.0.0")
                        .description("基于 Spring Boot 3 + Vue 3 的全栈房屋租赁系统后端接口文档\n\n"
                                + "角色说明：0-管理员 | 1-房东 | 2-租客")
                        .contact(new Contact()
                                .name("AnJu Team")
                                .email("support@anju-rental.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .schemaRequirement("Bearer", new SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("登录后获得的 JWT Token，直接填入即可（无需 Bearer 前缀也会自动处理）"));
    }
}