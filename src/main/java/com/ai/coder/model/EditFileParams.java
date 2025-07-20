package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 编辑文件参数
 */
@Data
public class EditFileParams {
    // Getters and Setters
    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("old_str")
    private String oldStr;

    @JsonProperty("new_str")
    private String newStr;

    @JsonProperty("start_line")
    private Integer startLine;

    @JsonProperty("end_line")
    private Integer endLine;

    // 构造器
    public EditFileParams() {
    }

    public EditFileParams(String filePath, String oldStr, String newStr) {
        this.filePath = filePath;
        this.oldStr = oldStr;
        this.newStr = newStr;
    }

    @Override
    public String toString() {
        return String.format("EditFileParams{path='%s', oldStrLength=%d, newStrLength=%d, lines=%s-%s}",
                filePath,
                oldStr != null ? oldStr.length() : 0,
                newStr != null ? newStr.length() : 0,
                startLine, endLine);
    }
}