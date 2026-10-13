package com.hkd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频评论，childComments 用于承载回复（查询时组装成两级结构）。
 */
@Data
public class CommentInfo {

    private Long id;

    private Long videoId;

    /** 父评论 ID，一级评论为 null */
    private Long parentCommentId;

    /** 展示昵称，取自登录用户的 displayName */
    private String commenter;

    private String comment;

    private LocalDateTime createTime;

    private List<CommentInfo> childComments = new ArrayList<>();

    /** 评论发布者用户 ID，服务端登记，不对外序列化 */
    @JsonIgnore
    private Long ownerId;

    /** 是否由当前登录用户发布，读取时按请求计算 */
    private boolean mine;
}