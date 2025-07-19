package com.ai.coder.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目上下文
 */
@Data
public class ProjectContext {
    private Path projectRoot;
    private ProjectType projectType;
    private ProjectStructure projectStructure;
    private List<DependencyInfo> dependencies;
    private List<ConfigFile> configFiles;
    private CodeStatistics codeStatistics;
    private Map<String, Object> metadata;
    private String contextSummary;

    public ProjectContext() {
        this.dependencies = new ArrayList<>();
        this.configFiles = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public ProjectContext(Path projectRoot) {
        this();
        this.projectRoot = projectRoot;
    }


    /**
     * 依赖信息类
     */
    @Data
    public static class DependencyInfo {
        private String name;
        private String version;
        private String type; // "compile", "test", "runtime", etc.
        private String scope;
        private boolean isDirectDependency;

        public DependencyInfo(String name, String version, String type) {
            this.name = name;
            this.version = version;
            this.type = type;
            this.isDirectDependency = true;
        }

        @Override
        public String toString() {
            return String.format("%s:%s (%s)", name, version, type);
        }
    }


    /**
     * 配置文件类
     */
    @Data
    public static class ConfigFile {
        private String fileName;
        private String relativePath;
        private String fileType; // "properties", "yaml", "json", "xml", etc.
        private Map<String, Object> keySettings;
        private boolean isMainConfig;

        public ConfigFile(String fileName, String relativePath, String fileType) {
            this.fileName = fileName;
            this.relativePath = relativePath;
            this.fileType = fileType;
            this.keySettings = new HashMap<>();
            this.isMainConfig = false;
        }

        public void addSetting(String key, Object value) {
            this.keySettings.put(key, value);
        }
    }

    /**
     * 代码统计类
     */
    @Data
    public static class CodeStatistics {
        private int totalLines;
        private int codeLines;
        private int commentLines;
        private int blankLines;
        private Map<String, Integer> languageLines;
        private int totalClasses;
        private int totalMethods;
        private int totalFunctions;

        public CodeStatistics() {
            this.languageLines = new HashMap<>();
        }

        public void addLanguageLines(String language, int lines) {
            this.languageLines.put(language, this.languageLines.getOrDefault(language, 0) + lines);
        }
    }


    /**
     * 生成项目上下文摘要
     * @return
     */
    public String generateContextSummary() {
        StringBuilder summary = new StringBuilder();

        // Basic information
        summary.append("=== PROJECT CONTEXT ===\n");
        summary.append("Project: ").append(projectRoot != null ? projectRoot.getFileName() : "Unknown").append("\n");
        summary.append("Type: ").append(projectType != null ? projectType.getDisplayName() : "Unknown").append("\n");
        summary.append("Language: ").append(projectType != null ? projectType.getPrimaryLanguage() : "Unknown").append("\n");
        summary.append("Package Manager: ").append(projectType != null ? projectType.getPackageManager() : "Unknown").append("\n\n");

        // Project structure
        if (projectStructure != null) {
            summary.append("=== PROJECT STRUCTURE ===\n");
            summary.append(projectStructure.getStructureSummary()).append("\n");
        }

        // Dependencies
        if (!dependencies.isEmpty()) {
            summary.append("=== DEPENDENCIES ===\n");
            dependencies.stream()
                    .filter(DependencyInfo::isDirectDependency)
                    .limit(10)
                    .forEach(dep -> summary.append("- ").append(dep.toString()).append("\n"));
            if (dependencies.size() > 10) {
                summary.append("... and ").append(dependencies.size() - 10).append(" more dependencies\n");
            }
            summary.append("\n");
        }

        // Configuration files
        if (!configFiles.isEmpty()) {
            summary.append("=== CONFIGURATION FILES ===\n");
            configFiles.stream()
                    .filter(ConfigFile::isMainConfig)
                    .forEach(config -> summary.append("- ").append(config.getFileName())
                            .append(" (").append(config.getFileType()).append(")\n"));
            summary.append("\n");
        }

        // Code statistics
        if (codeStatistics != null) {
            summary.append("=== CODE STATISTICS ===\n");
            summary.append("Total Lines: ").append(codeStatistics.getTotalLines()).append("\n");
            summary.append("Code Lines: ").append(codeStatistics.getCodeLines()).append("\n");
            if (codeStatistics.getTotalClasses() > 0) {
                summary.append("Classes: ").append(codeStatistics.getTotalClasses()).append("\n");
            }
            if (codeStatistics.getTotalMethods() > 0) {
                summary.append("Methods: ").append(codeStatistics.getTotalMethods()).append("\n");
            }
            summary.append("\n");
        }

        this.contextSummary = summary.toString();
        return this.contextSummary;
    }


    /**
     * 获取依赖摘要
     * @return
     */
    public String getDependencySummary() {
        if (dependencies.isEmpty()) {
            return "No dependencies found";
        }

        return dependencies.stream()
                .filter(DependencyInfo::isDirectDependency)
                .limit(5)
                .map(DependencyInfo::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("No direct dependencies");
    }

}
