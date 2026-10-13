package com.hkd.service.impl;

import com.hkd.model.User;
import com.hkd.exception.BusinessException;
import com.hkd.repository.UserRepository;
import com.hkd.service.UserService;
import com.hkd.util.PasswordHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 20;
    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 64;
    private static final int DISPLAY_MAX = 20;

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User register(String username, String password, String displayName) {
        String name = validateUsername(username);
        validatePassword(password);
        String shown = normalizeDisplayName(displayName, name);

        if (userRepository.existsByUsername(name)) {
            throw new BusinessException("用户名已被占用");
        }

        User user = new User();
        user.setUsername(name);
        user.setDisplayName(shown);
        user.setPasswordHash(PasswordHasher.hash(password));
        user.setCreateTime(LocalDateTime.now());
        try {
            User saved = userRepository.insert(user);
            log.info("注册新用户：id={}, username={}", saved.getId(), saved.getUsername());
            return saved;
        } catch (DuplicateKeyException e) {
            // 并发注册同名时唯一索引兜底
            throw new BusinessException("用户名已被占用");
        }
    }

    @Override
    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        return userRepository.findByUsername(username.trim())
                .filter(user -> PasswordHasher.matches(password, user.getPasswordHash()))
                .orElse(null);
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    private String validateUsername(String username) {
        String name = username == null ? "" : username.trim();
        if (name.length() < USERNAME_MIN || name.length() > USERNAME_MAX) {
            throw new BusinessException("用户名长度需在 " + USERNAME_MIN + "-" + USERNAME_MAX + " 位之间");
        }
        if (!USERNAME_PATTERN.matcher(name).matches()) {
            throw new BusinessException("用户名只能包含字母、数字和下划线");
        }
        return name;
    }

    private void validatePassword(String password) {
        String pwd = password == null ? "" : password;
        if (pwd.length() < PASSWORD_MIN || pwd.length() > PASSWORD_MAX) {
            throw new BusinessException("密码长度需在 " + PASSWORD_MIN + "-" + PASSWORD_MAX + " 位之间");
        }
    }

    private String normalizeDisplayName(String displayName, String username) {
        String shown = displayName == null ? "" : displayName.trim();
        if (shown.isEmpty()) {
            return username;
        }
        return shown.length() > DISPLAY_MAX ? shown.substring(0, DISPLAY_MAX) : shown;
    }
}