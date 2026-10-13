package com.hkd.service;

import com.hkd.model.User;

/**
 * 用户账号服务：注册、认证、查询。
 */
public interface UserService {

    /** 注册新用户，用户名重复 / 格式不合法时抛 BusinessException */
    User register(String username, String password, String displayName);

    /** 校验用户名密码，成功返回用户，失败返回 null */
    User authenticate(String username, String password);

    /** 按 ID 查询，不存在返回 null */
    User findById(Long id);
}