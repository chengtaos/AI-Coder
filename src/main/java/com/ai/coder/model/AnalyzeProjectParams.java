package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 分析项目参数
 */
@Data
public class AnalyzeProjectParams {
    // Getters and Setters
    @JsonProperty("project_path")
    private String projectPath;

    @JsonProperty("analysis_depth")
    private String analysisDepth = "detailed";

    @JsonProperty("include_code_stats")
    private Boolean includeCodeStats;

    @JsonProperty("output_format")
    private String outputFormat = "detailed";

    @Override
    public String toString() {
        return String.format("AnalyzeProjectParams{path='%s', depth='%s', format='%s'}",
                projectPath, analysisDepth, outputFormat);
    }
}