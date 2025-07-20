package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 文件差异结果
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDiff {
    private final String fileDiff;
    private final String fileName;

    public FileDiff(String fileDiff, String fileName) {
        this.fileDiff = fileDiff;
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "FileDiff{fileName='" + fileName + "'}";
    }
}