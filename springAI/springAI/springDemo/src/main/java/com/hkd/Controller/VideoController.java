package com.hkd.controller;

import com.hkd.model.VideoInfo;
import com.hkd.service.CommentService;
import com.hkd.service.VideoService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 视频库接口：前端的"视频库"页与"视频详情"页都走这里。
 */
@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;
    private final CommentService commentService;

    public VideoController(VideoService videoService, CommentService commentService) {
        this.videoService = videoService;
        this.commentService = commentService;
    }

    /** 视频列表 */
    @GetMapping
    public List<VideoInfo> list() {
        return videoService.list();
    }

    /** 视频详情 */
    @GetMapping("/{id}")
    public VideoInfo get(@PathVariable Long id) {
        return videoService.get(id);
    }

    /** 上传新视频（multipart/form-data：videoName + file + image） */
    @PostMapping("/upload")
    public VideoInfo upload(@RequestParam("videoName") String videoName,
                            @RequestParam(value = "file", required = false) MultipartFile file,
                            @RequestParam(value = "image", required = false) MultipartFile image) {
        return videoService.create(videoName, file, image);
    }

    /** 修改视频，不传文件表示只改名称 */
    @PutMapping("/{id}")
    public VideoInfo update(@PathVariable Long id,
                            @RequestParam("videoName") String videoName,
                            @RequestParam(value = "file", required = false) MultipartFile file,
                            @RequestParam(value = "image", required = false) MultipartFile image) {
        return videoService.update(id, videoName, file, image);
    }

    /** 删除视频，同时清掉它的评论 */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        videoService.delete(id);
        commentService.deleteByVideo(id);
    }
}
