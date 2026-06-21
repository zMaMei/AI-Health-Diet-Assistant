package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.UserProfileUpdateCommand;
import com.health.diet.dto.vo.UserProfileVO;
import com.health.diet.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /* 查询用户档案 */
    @GetMapping
    public ApiResponse<UserProfileVO> getProfile(HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 调用查询用户档案服务 */
        return ApiResponse.success(userProfileService.getProfile(userId));
    }

    /* 更新用户档案 */
    @PutMapping
    public ApiResponse<Void> updateProfile(@RequestBody UserProfileUpdateCommand command,
                                            HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 调用更新用户档案服务 */
        userProfileService.updateProfile(userId, command);
        return ApiResponse.success();
    }
}
