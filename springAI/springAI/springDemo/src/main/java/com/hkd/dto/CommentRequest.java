package com.hkd.dto;

import lombok.Data;

/**
 * 发表评论 / 回复的请求体。
 */
@Data
public class CommentRequest {

    private Long videoId;

    /** 回复某条评论时传入，一级评论留空 */
    private Long parentCommentId;

    private String comment;

    /** 昵称，未登录场景由前端生成并保存在本地，留空时后端填"匿名用户" */
    private String commenter;
}
