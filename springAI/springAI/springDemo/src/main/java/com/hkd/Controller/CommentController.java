package com.hkd.controller;

import com.hkd.model.CommentInfo;
import com.hkd.dto.CommentRequest;
import com.hkd.service.CommentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 视频评论接口。
 */
@RestController
@RequestMapping("/video-comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** 某个视频下的评论（含回复） */
    @GetMapping("/{videoId}")
    public List<CommentInfo> list(@PathVariable Long videoId) {
        return commentService.listByVideo(videoId);
    }

    /** 发表评论或回复 */
    @PostMapping
    public CommentInfo add(@RequestBody CommentRequest request) {
        return commentService.add(request);
    }

    /** 删除评论 */
    @DeleteMapping("/{commentId}")
    public void delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
    }
}
