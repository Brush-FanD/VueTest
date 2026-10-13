package com.hkd.dto;

import lombok.Data;

/**
 * 发表评论 / 回复的请求体。
 *
 * <p>注意：评论者昵称不再由前端传，统一取登录用户的 displayName，
 * 避免前端伪造昵称冒充别人。</p>
 */
@Data
public class CommentRequest {

    private Long videoId;

    /** 回复某条评论时传入，一级评论留空 */
    private Long parentCommentId;

    private String comment;
}