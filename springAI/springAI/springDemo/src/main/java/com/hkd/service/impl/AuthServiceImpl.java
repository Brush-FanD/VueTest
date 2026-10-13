package com.hkd.service.impl;

import com.hkd.model.User;
import com.hkd.exception.BusinessException;
import com.hkd.exception.UnauthorizedException;
import com.hkd.service.AuthService;
import com.hkd.service.UserService;
import com.hkd.session.SessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /** sessionId -> userId，只存在服务端内存；重启后需要重新登录 */
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

    private final UserService userService;

    public AuthServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public User login(String username, String password) {
        User user = userService.authenticate(username, password);
        if (user == null) {
            throw new BusinessException("用户名或密码不正确");
        }
        sessionToUser.put(SessionContext.require(), user.getId());
        log.info("用户登录：id={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    @Override
    public void logout() {
        Long userId = sessionToUser.remove(SessionContext.require());
        if (userId != null) {
            log.info("用户登出：id={}", userId);
        }
    }

    @Override
    public User currentUser() {
        Long userId = sessionToUser.get(SessionContext.require());
        return userId == null ? null : userService.findById(userId);
    }

    @Override
    public User requireUser() {
        User user = currentUser();
        if (user == null) {
            throw new UnauthorizedException("请先登录后再操作");
        }
        return user;
    }
}