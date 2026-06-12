package com.health.diet.service;

import com.health.diet.dto.command.LoginCommand;
import com.health.diet.dto.command.RegisterCommand;
import com.health.diet.dto.vo.LoginResultVO;
import com.health.diet.entity.User;
import com.health.diet.entity.UserProfile;
import com.health.diet.repository.UserProfileRepository;
import com.health.diet.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // token → userId
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Path AVATAR_DIR = Paths.get("uploads", "avatars");

    public AuthService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    // ==================== 注册 ====================

    public LoginResultVO register(RegisterCommand command) {
        String username = command.getUsername().trim();
        String password = command.getPassword().trim();

        // 校验
        if (username.length() < 2) {
            throw new IllegalArgumentException("用户名至少 2 个字符");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 个字符");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 创建用户
        String hash = passwordEncoder.encode(password);
        User user = new User(username, hash, username); // 默认昵称=用户名
        userRepository.save(user);

        // 创建默认档案
        UserProfile profile = new UserProfile(user.getId(), "均衡");
        userProfileRepository.save(profile);

        log.info("新用户注册: id={}, username={}", user.getId(), username);

        // 自动登录
        return buildLoginResult(user, null);
    }

    // ==================== 登录 ====================

    public LoginResultVO login(LoginCommand command) {
        String username = command.getUsername().trim();
        String password = command.getPassword().trim();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        log.info("用户登录: id={}, username={}", user.getId(), username);

        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        return buildLoginResult(user, profile);
    }

    // ==================== 登出 ====================

    public void logout(String token) {
        if (token != null) {
            tokenStore.remove(token);
            log.info("用户登出: token={}", token.substring(0, Math.min(8, token.length())) + "...");
        }
    }

    // ==================== Token 验证 ====================

    public Long getUserIdFromToken(String token) {
        if (token == null) return null;
        return tokenStore.get(token);
    }

    // ==================== 头像上传 ====================

    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        // 校验类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片文件");
        }
        String ext = switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("不支持的图片格式：" + contentType);
        };

        // 校验大小（≤5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("头像大小不能超过 5MB");
        }

        // 保存文件
        Path userDir = AVATAR_DIR.resolve(String.valueOf(userId));
        Files.createDirectories(userDir);

        // 删除旧头像文件
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile != null && profile.getAvatarUrl() != null) {
            try {
                String oldPath = profile.getAvatarUrl().replace("/api/uploads/", "");
                Files.deleteIfExists(Paths.get(oldPath));
            } catch (IOException e) {
                log.warn("删除旧头像失败", e);
            }
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = userDir.resolve(filename);
        // 使用 Files.copy 而非 file.transferTo，避免 Tomcat ApplicationPart.write 路径解析不一致
        try (java.io.InputStream in = file.getInputStream()) {
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // 更新数据库
        String avatarUrl = "/api/uploads/avatars/" + userId + "/" + filename;
        if (profile == null) {
            profile = new UserProfile(userId, "均衡");
            profile = userProfileRepository.save(profile);
        }
        profile.setAvatarUrl(avatarUrl);
        userProfileRepository.save(profile);

        log.info("头像上传成功: userId={}, path={}", avatarUrl);
        return avatarUrl;
    }

    // ==================== 内部方法 ====================

    private LoginResultVO buildLoginResult(User user, UserProfile profile) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, user.getId());

        LoginResultVO vo = new LoginResultVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(profile != null ? profile.getAvatarUrl() : null);
        vo.setToken(token);
        return vo;
    }
}
