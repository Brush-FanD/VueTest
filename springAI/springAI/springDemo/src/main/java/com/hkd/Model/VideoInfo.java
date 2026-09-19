package com.hkd.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 视频信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoInfo {

    private Long id;

    private String videoName;

    /** 封面图访问路径，未上传封面时为 null */
    private String image;

    /** 视频访问路径，例如 /api/files/video-xxx.mp4 */
    private String videoPath;

    private LocalDateTime createTime;
}
