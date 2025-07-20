package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

/**
 * 脚手架参数类
 */
@Data
public  class ScaffoldParams {
    // Getters and Setters
    @JsonProperty("project_name")
    private String projectName;

    @JsonProperty("project_type")
    private String projectType;

    @JsonProperty("project_path")
    private String projectPath;

    @JsonProperty("template_variables")
    private Map<String, Object> templateVariables;

    @JsonProperty("include_git")
    private Boolean includeGit = true;

    @JsonProperty("include_readme")
    private Boolean includeReadme = true;

    @JsonProperty("include_gitignore")
    private Boolean includeGitignore = true;

}