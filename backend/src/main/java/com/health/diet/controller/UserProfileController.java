package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.UserProfileUpdateCommand;
import com.health.diet.dto.vo.UserProfileVO;
import com.health.diet.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<UserProfileVO> getProfile(@RequestParam Long userId) {
        return ApiResponse.success(userProfileService.getProfile(userId));
    }

    @PutMapping
    public ApiResponse<Void> updateProfile(@RequestParam Long userId,
                                            @RequestBody UserProfileUpdateCommand command) {
        userProfileService.updateProfile(userId, command);
        return ApiResponse.success();
    }
}
