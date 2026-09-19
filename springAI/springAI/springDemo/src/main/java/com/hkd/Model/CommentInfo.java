package com.hkd.model;

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

    private String commenter;

    private String comment;

    private LocalDateTime createTime;

    private List<CommentInfo> childComments = new ArrayList<>();
}
