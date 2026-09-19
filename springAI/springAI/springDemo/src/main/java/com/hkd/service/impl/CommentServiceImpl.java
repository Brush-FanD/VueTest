package com.hkd.service.impl;

import com.hkd.model.CommentInfo;
import com.hkd.dto.CommentRequest;
import com.hkd.exception.BusinessException;
import com.hkd.service.CommentService;
import com.hkd.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评论服务实现：内存存储，重启即清空。
 */
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private static final int MAX_COMMENT_LENGTH = 500;
    private static final int MAX_COMMENTER_LENGTH = 20;
    private static final String DEFAULT_COMMENTER = "匿名用户";

    private final Map<Long, CommentInfo> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    private final VideoService videoService;

    public CommentServiceImpl(VideoService videoService) {
        this.videoService = videoService;
    }

    @Override
    public List<CommentInfo> listByVideo(Long videoId) {
        // 视频不存在时直接报错，避免前端拿着错误 ID 一直请求
        videoService.get(videoId);

        List<CommentInfo> all = store.values().stream()
                .filter(comment -> videoId.equals(comment.getVideoId()))
                .sorted(Comparator.comparing(CommentInfo::getId).reversed())
                .toList();

        Map<Long, CommentInfo> index = new HashMap<>();
        for (CommentInfo comment : all) {
            comment.setChildComments(new ArrayList<>());
            index.put(comment.getId(), comment);
        }

        List<CommentInfo> roots = new ArrayList<>();
        for (CommentInfo comment : all) {
            CommentInfo parent = comment.getParentCommentId() == null ? null : index.get(comment.getParentCommentId());
            if (parent == null) {
                roots.add(comment);
            } else {
                parent.getChildComments().add(comment);
            }
        }
        return roots;
    }

    @Override
    public CommentInfo add(CommentRequest request) {
        if (request == null || request.getVideoId() == null) {
            throw new BusinessException("缺少视频 ID");
        }
        videoService.get(request.getVideoId());

        String content = request.getComment() == null ? "" : request.getComment().trim();
        if (content.isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessException("评论内容不能超过 " + MAX_COMMENT_LENGTH + " 个字");
        }

        CommentInfo comment = new CommentInfo();
        comment.setId(idGenerator.incrementAndGet());
        comment.setVideoId(request.getVideoId());
        comment.setParentCommentId(normalizeParentId(request.getParentCommentId()));
        comment.setCommenter(normalizeCommenter(request.getCommenter()));
        comment.setComment(content);
        comment.setCreateTime(LocalDateTime.now());
        comment.setChildComments(new ArrayList<>());
        store.put(comment.getId(), comment);
        log.info("新增评论：id={}, videoId={}, parentId={}", comment.getId(), comment.getVideoId(), comment.getParentCommentId());
        return comment;
    }

    @Override
    public void delete(Long commentId) {
        if (commentId == null || store.remove(commentId) == null) {
            throw new BusinessException("评论不存在");
        }
        deleteReplies(commentId);
    }

    @Override
    public void deleteByVideo(Long videoId) {
        store.values().removeIf(comment -> videoId.equals(comment.getVideoId()));
    }

    /**
     * 只保留两级评论：回复"回复"时自动挂到它所属的一级评论下，避免无限嵌套把界面撑破。
     */
    private Long normalizeParentId(Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }
        CommentInfo parent = store.get(parentCommentId);
        if (parent == null) {
            throw new BusinessException("要回复的评论不存在");
        }
        return parent.getParentCommentId() == null ? parent.getId() : parent.getParentCommentId();
    }

    private String normalizeCommenter(String commenter) {
        String name = commenter == null ? "" : commenter.trim();
        if (name.isEmpty()) {
            return DEFAULT_COMMENTER;
        }
        return name.length() > MAX_COMMENTER_LENGTH ? name.substring(0, MAX_COMMENTER_LENGTH) : name;
    }

    private void deleteReplies(Long parentId) {
        List<Long> childIds = store.values().stream()
                .filter(comment -> parentId.equals(comment.getParentCommentId()))
                .map(CommentInfo::getId)
                .toList();
        for (Long childId : childIds) {
            store.remove(childId);
            deleteReplies(childId);
        }
    }
}
