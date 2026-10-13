package com.hkd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户账号。对应数据库表 app_user，由 UserRepository 用 JdbcTemplate 读写。
 */
@Data
@NoArgsConstructor
public class User {

    private Long id;

    /** 登录名，全局唯一（不区分大小写） */
    private String username;

    /** 展示昵称，评论 / 界面显示用 */
    private String displayName;

    /** PBKDF2 哈希（含盐与迭代次数），永远不返回给前端 */
    @JsonIgnore
    private String passwordHash;

    private LocalDateTime createTime;
}