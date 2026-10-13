package com.hkd.service.impl;

import com.hkd.model.User;
import com.hkd.model.VideoInfo;
import com.hkd.exception.BusinessException;
import com.hkd.exception.ForbiddenException;
import com.hkd.service.AuthService;
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
 * 后续接入数据库时只需要替换 store 这一层。</p>
 *
 * <p>归属：视频列表对所有人可见（内容型产品），但上传 / 修改 / 删除都要求登录，
 * 且只有作者本人（{@link #requireOwner}）能改自己的视频。作者身份取自服务端登录态。</p>
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
    private final AuthService authService;

    public VideoServiceImpl(FileStorage fileStorage, AuthService authService) {
        this.fileStorage = fileStorage;
        this.authService = authService;
    }

    @Override
    public List<VideoInfo> list() {
        return store.values().stream()
                .sorted(Comparator.comparing(VideoInfo::getId).reversed())
                .map(this::toView)
                .toList();
    }

    @Override
    public VideoInfo get(Long id) {
        return toView(find(id));
    }

    @Override
    public VideoInfo create(String videoName, MultipartFile videoFile, MultipartFile imageFile) {
        User user = authService.requireUser();
        String name = validateName(videoName);
        validateVideoFile(videoFile, true);
        validateImageFile(imageFile);

        String videoPath = fileStorage.save(videoFile, "video");
        String image = fileStorage.save(imageFile, "image");

        VideoInfo video = new VideoInfo();
        video.setId(idGenerator.incrementAndGet());
        video.setVideoName(name);
        video.setImage(image);
        video.setVideoPath(videoPath);
        video.setCreateTime(LocalDateTime.now());
        video.setOwnerId(user.getId());
        store.put(video.getId(), video);
        log.info("新增视频：id={}, name={}, ownerId={}", video.getId(), name, user.getId());
        return toView(video);
    }

    @Override
    public VideoInfo update(Long id, String videoName, MultipartFile videoFile, MultipartFile imageFile) {
        VideoInfo video = find(id);
        requireOwner(video);
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
        return toView(video);
    }

    @Override
    public void delete(Long id) {
        VideoInfo video = find(id);
        requireOwner(video);
        store.remove(id);
        fileStorage.delete(video.getVideoPath());
        fileStorage.delete(video.getImage());
        log.info("删除视频：id={}", id);
    }

    /** 内部用：拿存储里的原始对象，不存在时抛业务异常 */
    private VideoInfo find(Long id) {
        VideoInfo video = id == null ? null : store.get(id);
        if (video == null) {
            throw new BusinessException("视频不存在：" + id);
        }
        return video;
    }

    /** 归属校验：不是作者就 403 */
    private void requireOwner(VideoInfo video) {
        if (!isOwner(video)) {
            throw new ForbiddenException("只能修改或删除自己上传的视频");
        }
    }

    private boolean isOwner(VideoInfo video) {
        Long owner = video.getOwnerId();
        if (owner == null) {
            return false;
        }
        User current = authService.currentUser();
        return current != null && owner.equals(current.getId());
    }

    /**
     * 对外视图：复制一份并带上 mine 标记，避免把 ownerId 泄露给前端，
     * 也避免并发请求互相改写共享对象上的 mine 字段。
     */
    private VideoInfo toView(VideoInfo source) {
        VideoInfo view = new VideoInfo();
        view.setId(source.getId());
        view.setVideoName(source.getVideoName());
        view.setImage(source.getImage());
        view.setVideoPath(source.getVideoPath());
        view.setCreateTime(source.getCreateTime());
        view.setMine(isOwner(source));
        return view;
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