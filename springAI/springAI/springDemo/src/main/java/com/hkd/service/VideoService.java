package com.hkd.service;

import com.hkd.model.VideoInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 视频库服务。
 */
public interface VideoService {

    /** 按上传时间倒序返回全部视频 */
    List<VideoInfo> list();

    /** 查询单个视频，不存在时抛 BusinessException */
    VideoInfo get(Long id);

    /** 新增视频，videoFile 必传，imageFile 可选 */
    VideoInfo create(String videoName, MultipartFile videoFile, MultipartFile imageFile);

    /** 修改视频，文件传 null 表示保持原文件不变 */
    VideoInfo update(Long id, String videoName, MultipartFile videoFile, MultipartFile imageFile);

    /** 删除视频，同时清理磁盘上的视频与封面文件 */
    void delete(Long id);
}
