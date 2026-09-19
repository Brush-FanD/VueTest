package com.hkd.service.impl;

import com.hkd.model.VideoInfo;
import com.hkd.exception.BusinessException;
import com.hkd.service.FileStorage;
import com.hkd.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 视频库服务实现。
 *
 * <p>当前用内存 Map 存元数据、本地磁盘存文件，重启后元数据会清空；
 * 后续接入 MySQL/Mongo 时只需要替换 store 这一层，Controller 与前端都不用改。</p>
 */
@Slf4j
@Service
public class VideoServiceImpl implements VideoService {

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mp4", "mov", "m4v", "webm", "avi", "mkv");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "bmp");
    private static final int MAX_NAME_LENGTH = 100;

    private final Map<Long, VideoInfo> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    private final FileStorage fileStorage;

    public VideoServiceImpl(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Override
    public List<VideoInfo> list() {
        return store.values().stream()
                .sorted(Comparator.comparing(VideoInfo::getId).reversed())
                .toList();
    }

    @Override
    public VideoInfo get(Long id) {
        VideoInfo video = id == null ? null : store.get(id);
        if (video == null) {
            throw new BusinessException("视频不存在：" + id);
        }
        return video;
    }

    @Override
    public VideoInfo create(String videoName, MultipartFile videoFile, MultipartFile imageFile) {
        String name = validateName(videoName);
        validateVideoFile(videoFile, true);
        validateImageFile(imageFile);

        String videoPath = fileStorage.save(videoFile, "video");
        String image = fileStorage.save(imageFile, "image");

        VideoInfo video = new VideoInfo(idGenerator.incrementAndGet(), name, image, videoPath, LocalDateTime.now());
        store.put(video.getId(), video);
        log.info("新增视频：id={}, name={}", video.getId(), name);
        return video;
    }

    @Override
    public VideoInfo update(Long id, String videoName, MultipartFile videoFile, MultipartFile imageFile) {
        VideoInfo video = get(id);
        String name = validateName(videoName);
        validateVideoFile(videoFile, false);
        validateImageFile(imageFile);

        String newVideoPath = fileStorage.save(videoFile, "video");
        if (newVideoPath != null) {
            fileStorage.delete(video.getVideoPath());
            video.setVideoPath(newVideoPath);
        }
        String newImage = fileStorage.save(imageFile, "image");
        if (newImage != null) {
            fileStorage.delete(video.getImage());
            video.setImage(newImage);
        }
        video.setVideoName(name);
        log.info("修改视频：id={}, name={}", id, name);
        return video;
    }

    @Override
    public void delete(Long id) {
        VideoInfo video = id == null ? null : store.remove(id);
        if (video == null) {
            throw new BusinessException("视频不存在：" + id);
        }
        fileStorage.delete(video.getVideoPath());
        fileStorage.delete(video.getImage());
        log.info("删除视频：id={}", id);
    }

    private String validateName(String videoName) {
        String name = videoName == null ? "" : videoName.trim();
        if (name.isEmpty()) {
            throw new BusinessException("视频名称不能为空");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("视频名称不能超过 " + MAX_NAME_LENGTH + " 个字");
        }
        return name;
    }

    private void validateVideoFile(MultipartFile file, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) {
                throw new BusinessException("请选择要上传的视频文件");
            }
            return;
        }
        checkExtension(file, ALLOWED_VIDEO_EXTENSIONS, "视频仅支持 mp4 / mov / m4v / webm / avi / mkv 格式");
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        checkExtension(file, ALLOWED_IMAGE_EXTENSIONS, "封面仅支持 jpg / jpeg / png / webp / gif / bmp 格式");
    }

    private void checkExtension(MultipartFile file, Set<String> allowedExtensions, String message) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
            throw new BusinessException(message);
        }
    }
}
