package com.health.diet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/* Web MVC配置 */
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public WebMvcConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    /* 静态资源映射 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /* 上传文件访问路径映射 */
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    /* 注册登录拦截器 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                /* 放行路径（登录/注册无需认证） */
                .excludePathPatterns("/api/auth/register", "/api/auth/login", "/api/uploads/**");
    }
}
