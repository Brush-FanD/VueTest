package com.hkd.service;

import com.hkd.model.User;

/**
 * 登录态服务：把「服务端会话」和「用户」关联起来。
 *
 * <p>Cookie 里始终只放随机会话 ID，userId 只存在服务端内存映射里，
 * 因此客户端无法通过改 Cookie 伪造身份。</p>
 */
public interface AuthService {

    /** 校验账号密码并绑定到当前会话，失败抛 BusinessException */
    User login(String username, String password);

    /** 解除当前会话与用户的绑定 */
    void logout();

    /** 当前登录用户，未登录返回 null */
    User currentUser();

    /** 当前登录用户，未登录抛 UnauthorizedException(401) */
    User requireUser();
}