package com.hkd.service;

import com.hkd.model.CommentInfo;
import com.hkd.dto.CommentRequest;

import java.util.List;

/**
 * 视频评论服务。
 */
public interface CommentService {

    /** 按视频查询评论树（一级评论与其回复均为新评论在前） */
    List<CommentInfo> listByVideo(Long videoId);

    /** 发表评论或回复 */
    CommentInfo add(CommentRequest request);

    /** 删除评论，连同其下所有回复一起删除 */
    void delete(Long commentId);

    /** 视频被删除时清理其全部评论 */
    void deleteByVideo(Long videoId);
}
