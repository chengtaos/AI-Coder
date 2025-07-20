package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 读取文件参数
 */
public class ReadFileParams {
    @JsonProperty("absolute_path")
    private String absolutePath;

    private Integer offset;
    private Integer limit;

    // 构造器
    public ReadFileParams() {
    }

    public ReadFileParams(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public ReadFileParams(String absolutePath, Integer offset, Integer limit) {
        this.absolutePath = absolutePath;
        this.offset = offset;
        this.limit = limit;
    }

    // Getters and Setters
    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return String.format("ReadFileParams{path='%s', offset=%d, limit=%d}",
                absolutePath, offset, limit);
    }
}