package com.hkd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 视频信息。
 */
@Data
@NoArgsConstructor
public class VideoInfo {

    private Long id;

    private String videoName;

    /** 封面图访问路径，未上传封面时为 null */
    private String image;

    /** 视频访问路径，例如 /api/files/video-xxx.mp4 */
    private String videoPath;

    private LocalDateTime createTime;

    /**
     * 上传者用户 ID（登录用户）。用 @JsonIgnore 保证它不返回给前端，
     * 否则等于把别人的身份泄露出去。
     */
    @JsonIgnore
    private Long ownerId;

    /** 是否由当前登录用户上传；读取时按请求计算，不入库 */
    private boolean mine;
}