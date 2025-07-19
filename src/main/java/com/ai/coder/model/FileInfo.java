package com.ai.coder.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息
 */
@Data
public class FileInfo {
    private final String name;
    private final String relativePath;
    private final boolean isDirectory;
    private final long size;
    private final LocalDateTime lastModified;

    public FileInfo(String name, String relativePath, boolean isDirectory, long size, LocalDateTime lastModified) {
        this.name = name;
        this.relativePath = relativePath;
        this.isDirectory = isDirectory;
        this.size = size;
        this.lastModified = lastModified;
    }

}