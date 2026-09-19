package com.hkd.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 上传文件存储抽象：把 MultipartFile 落盘并返回可访问的 URL。
 * 以后要换成 OSS / MinIO，只需另写一个实现类。
 */
public interface FileStorage {

    /**
     * 保存文件。
     *
     * @param file     上传的文件，为空时直接返回 null
     * @param category 文件分类，用于拼接文件名前缀（video / image）
     * @return 可访问路径，例如 /api/files/video-1a2b3c.mp4
     */
    String save(MultipartFile file, String category);

    /**
     * 按访问路径删除文件，路径不存在或文件已被删除时静默返回。
     */
    void delete(String url);
}
