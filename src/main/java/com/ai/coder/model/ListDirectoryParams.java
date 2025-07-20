package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 列表目录参数
 */
@Data
public class ListDirectoryParams {
    private String path;
    private Boolean recursive;

    @JsonProperty("max_depth")
    private Integer maxDepth;

    @JsonProperty("show_hidden")
    private Boolean showHidden;

    // 构造器
    public ListDirectoryParams() {
    }

    public ListDirectoryParams(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return String.format("ListDirectoryParams{path='%s', recursive=%s, maxDepth=%d}",
                path, recursive, maxDepth);
    }
}