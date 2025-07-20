package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SmartEditParams {
    @JsonProperty("project_path")
    private String projectPath;

    @JsonProperty("edit_description")
    private String editDescription;

    @JsonProperty("target_files")
    private List<String> targetFiles;

    @JsonProperty("scope")
    private String scope = "related_files";

    @JsonProperty("dry_run")
    private Boolean dryRun = false;

}