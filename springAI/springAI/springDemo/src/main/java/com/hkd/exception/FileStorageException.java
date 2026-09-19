package com.hkd.exception;

/**
 * 文件存储异常：上传目录不可写、文件落盘失败等。
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
