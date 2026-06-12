package com.health.diet.service;

import com.health.diet.dto.command.UserProfileUpdateCommand;
import com.health.diet.dto.vo.UserProfileVO;
import com.health.diet.entity.User;
import com.health.diet.entity.UserProfile;
import com.health.diet.repository.UserProfileRepository;
import com.health.diet.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserRepository userRepository,
                               UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfileVO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setUsername(user.getUsername());

        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            vo.setAvatarUrl(profile.getAvatarUrl());
            vo.setId(profile.getId());
            vo.setAge(profile.getAge());
            vo.setHeightCm(profile.getHeightCm());
            vo.setWeightKg(profile.getWeightKg());
            vo.setGoal(profile.getGoal());
            vo.setTaboo(profile.getTaboo());
            vo.setTastePreference(profile.getTastePreference());
            vo.setWarningProfile(profile.getWarningProfile());
        });

        return vo;
    }

    public void updateProfile(Long userId, UserProfileUpdateCommand command) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile(userId, command.getGoal() != null ? command.getGoal() : "均衡");
                    return p;
                });

        if (command.getAge() != null) profile.setAge(command.getAge());
        if (command.getHeightCm() != null) profile.setHeightCm(command.getHeightCm());
        if (command.getWeightKg() != null) profile.setWeightKg(command.getWeightKg());
        if (command.getGoal() != null) profile.setGoal(command.getGoal());
        if (command.getTaboo() != null) profile.setTaboo(command.getTaboo());
        if (command.getTastePreference() != null) profile.setTastePreference(command.getTastePreference());
        if (command.getWarningProfile() != null) profile.setWarningProfile(command.getWarningProfile());

        userProfileRepository.save(profile);
    }
}
