package com.hkd.controller;

import com.hkd.dto.LoginRequest;
import com.hkd.dto.RegisterRequest;
import com.hkd.dto.Result;
import com.hkd.model.User;
import com.hkd.service.AuthService;
import com.hkd.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录 / 注册接口。登录态复用 HttpOnly 会话 Cookie，登录成功后由服务端绑定 userId。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    /** 注册并自动登录 */
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        userService.register(request.getUsername(), request.getPassword(), request.getDisplayName());
        User user = authService.login(request.getUsername(), request.getPassword());
        return Result.success(user);
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request.getUsername(), request.getPassword()));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success(null);
    }

    /** 当前登录用户，未登录时 data 为 null（前端据此判断要不要显示登录入口） */
    @GetMapping("/me")
    public Result<User> me() {
        return Result.success(authService.currentUser());
    }
}