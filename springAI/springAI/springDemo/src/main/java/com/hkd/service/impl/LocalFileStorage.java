package com.hkd.service.impl;

import com.hkd.exception.FileStorageException;
import com.hkd.service.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地磁盘存储实现：文件统一落在 app.upload-dir 目录，文件名用 UUID 重命名，避免中文名和重名问题。
 */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private static final int MAX_EXTENSION_LENGTH = 8;

    private final Path rootDir;
    private final String urlPrefix;

    public LocalFileStorage(@Value("${app.upload-dir:./uploads}") String uploadDir,
                            @Value("${server.servlet.context-path:}") String contextPath) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new FileStorageException("上传目录创建失败：" + rootDir, e);
        }
        this.urlPrefix = (contextPath == null ? "" : contextPath) + "/files";
        log.info("上传目录：{}，访问前缀：{}", rootDir, urlPrefix);
    }

    @Override
    public String save(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String fileName = category + "-" + UUID.randomUUID().toString().replace("-", "") + extensionOf(file.getOriginalFilename());
        try {
            file.transferTo(rootDir.resolve(fileName));
        } catch (IOException e) {
            throw new FileStorageException("文件保存失败：" + fileName, e);
        }
        return urlPrefix + "/" + fileName;
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        // 只取文件名，防止 ../ 之类的相对路径删掉上传目录以外的文件
        String fileName = Paths.get(url).getFileName().toString();
        Path target = rootDir.resolve(fileName).normalize();
        if (!target.startsWith(rootDir)) {
            log.warn("拒绝删除上传目录以外的文件：{}", target);
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // 删除失败不影响业务流程，只记录日志
            log.warn("删除文件失败：{}", target, e);
        }
    }

    private String extensionOf(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null) {
            return "";
        }
        String cleaned = extension.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        if (cleaned.isEmpty()) {
            return "";
        }
        return "." + cleaned.substring(0, Math.min(cleaned.length(), MAX_EXTENSION_LENGTH));
    }
}
