package com.health.diet.config;

import com.health.diet.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
/* 登录认证拦截器 */
public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoginInterceptor.class);

    private final AuthService authService;

    public LoginInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // CORS 预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 认证相关接口放行
        if (path.equals("/api/auth/register") || path.equals("/api/auth/login")) {
            return true;
        }

        // 静态资源放行
        if (path.startsWith("/api/uploads/")) {
            return true;
        }

        /* 从请求头提取Token */
        String authHeader = request.getHeader("Authorization");
        /* Bearer Token解析 */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("缺少 Authorization 头: {} {}", method, path);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
            return false;
        }

        String token = authHeader.substring(7);
        /* Token验证 */
        Long userId = authService.getUserIdFromToken(token);

        if (userId == null) {
            log.warn("无效 token: {} {}", method, path);
            /* 未登录返回401 */
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }

        /* 注入userId到请求属性 */
        request.setAttribute("userId", userId);
        return true;
    }
}
