package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.LoginCommand;
import com.health.diet.dto.command.RegisterCommand;
import com.health.diet.dto.vo.LoginResultVO;
import com.health.diet.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /* 用户注册 */
    @PostMapping("/register")
    public ApiResponse<LoginResultVO> register(@Valid @RequestBody RegisterCommand command) {
        /* 异常处理 */
        try {
            /* 调用用户注册服务 */
            LoginResultVO result = authService.register(command);
            log.info("注册成功: username={}, userId={}", command.getUsername(), result.getUserId());
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /* 用户登录 */
    @PostMapping("/login")
    public ApiResponse<LoginResultVO> login(@Valid @RequestBody LoginCommand command) {
        /* 异常处理 */
        try {
            /* 调用用户登录服务 */
            LoginResultVO result = authService.login(command);
            log.info("登录成功: username={}", command.getUsername());
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /* 用户登出 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        /* 从请求头提取Token */
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        /* 调用登出服务 */
        authService.logout(token);
        return ApiResponse.success();
    }

    /* 头像上传 */
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        /* 参数校验 */
        if (file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }

        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 异常处理 */
        try {
            /* 调用头像上传服务 */
            String avatarUrl = authService.uploadAvatar(userId, file);
            return ApiResponse.success(avatarUrl);
        } catch (IllegalArgumentException | IOException e) {
            log.error("头像上传失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
