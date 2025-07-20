package com.ai.coder.model;

import com.ai.coder.model.ProjectType;
import lombok.Getter;

import java.nio.file.Path;

/**
 * 脚手架结果类
 */
public class ScaffoldResult {
    private java.util.List<String> createdItems = new java.util.ArrayList<>();
    @Getter
    private String summary;
    @Getter
    private String details;

    public void addCreatedItem(String item) {
        createdItems.add(item);
    }

    /**
     * 生成摘要
     * @param projectPath
     * @param projectType
     */
    public void generateSummary(Path projectPath, ProjectType projectType) {
        this.summary = String.format("Created %s project '%s' with %d files/directories",
                projectType.getDisplayName(),
                projectPath.getFileName(),
                createdItems.size());

        StringBuilder detailsBuilder = new StringBuilder();
        detailsBuilder.append("Created project structure:\n");
        for (String item : createdItems) {
            detailsBuilder.append("✓ ").append(item).append("\n");
        }
        this.details = detailsBuilder.toString();
    }

}