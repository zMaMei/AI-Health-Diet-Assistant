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

    /** POST /api/auth/register — 注册成功自动登录 */
    @PostMapping("/register")
    public ApiResponse<LoginResultVO> register(@Valid @RequestBody RegisterCommand command) {
        try {
            LoginResultVO result = authService.register(command);
            log.info("注册成功: username={}, userId={}", command.getUsername(), result.getUserId());
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ApiResponse<LoginResultVO> login(@Valid @RequestBody LoginCommand command) {
        try {
            LoginResultVO result = authService.login(command);
            log.info("登录成功: username={}", command.getUsername());
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** POST /api/auth/logout */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        authService.logout(token);
        return ApiResponse.success();
    }

    /** POST /api/auth/avatar — 上传/更新头像 */
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }

        Long userId = (Long) request.getAttribute("userId");
        try {
            String avatarUrl = authService.uploadAvatar(userId, file);
            return ApiResponse.success(avatarUrl);
        } catch (IllegalArgumentException | IOException e) {
            log.error("头像上传失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
