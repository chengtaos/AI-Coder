package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 工具确认详情
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ToolConfirmationDetails {
    // Getters
    private final String type;
    private final String title;
    private final String description;
    private final Object details;

    public ToolConfirmationDetails(String type, String title, String description, Object details) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.details = details;
    }

    public static ToolConfirmationDetails edit(String title, String fileName, String fileDiff) {
        return new ToolConfirmationDetails("edit", title, "File edit confirmation",
                new FileDiff(fileDiff, fileName));
    }

    public static ToolConfirmationDetails exec(String title, String command) {
        return new ToolConfirmationDetails("exec", title, "Command execution confirmation", command);
    }

}
