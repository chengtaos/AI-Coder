package com.ai.coder.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目结构
 */
@Data
public class ProjectStructure {
    private Path projectRoot;
    private ProjectType projectType;
    private List<DirectoryInfo> directories;
    private Map<String, Integer> fileTypeCount;
    private List<String> keyFiles;
    private int totalFiles;
    private int totalDirectories;
    private long totalSize;

    public ProjectStructure() {
        this.directories = new ArrayList<>();
        this.fileTypeCount = new HashMap<>();
        this.keyFiles = new ArrayList<>();
    }

    public ProjectStructure(Path projectRoot, ProjectType projectType) {
        this();
        this.projectRoot = projectRoot;
        this.projectType = projectType;
    }


    @Data
    public static class DirectoryInfo {
        private String name;
        private String relativePath;
        private int fileCount;
        private List<String> files;
        private boolean isImportant; // Whether it's an important directory (like src, test, etc.)

        public DirectoryInfo(String name, String relativePath) {
            this.name = name;
            this.relativePath = relativePath;
            this.files = new ArrayList<>();
            this.isImportant = false;
        }

        public void addFile(String fileName) {
            this.files.add(fileName);
            this.fileCount++;
        }
    }

    /**
     * Add directory information
     */
    public void addDirectory(DirectoryInfo directoryInfo) {
        this.directories.add(directoryInfo);
        this.totalDirectories++;
    }

    /**
     * Add file type statistics
     */
    public void addFileType(String extension, int count) {
        this.fileTypeCount.put(extension, this.fileTypeCount.getOrDefault(extension, 0) + count);
    }

    /**
     * Add key file
     */
    public void addKeyFile(String fileName) {
        if (!this.keyFiles.contains(fileName)) {
            this.keyFiles.add(fileName);
        }
    }

    /**
     * Get project structure summary
     */
    public String getStructureSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Project: ").append(projectRoot != null ? projectRoot.getFileName() : "Unknown").append("\n");
        summary.append("Type: ").append(projectType != null ? projectType.getDisplayName() : "Unknown").append("\n");
        summary.append("Directories: ").append(totalDirectories).append("\n");
        summary.append("Files: ").append(totalFiles).append("\n");

        if (!keyFiles.isEmpty()) {
            summary.append("Key Files: ").append(String.join(", ", keyFiles)).append("\n");
        }

        if (!fileTypeCount.isEmpty()) {
            summary.append("File Types: ");
            fileTypeCount.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .forEach(entry -> summary.append(entry.getKey()).append("(").append(entry.getValue()).append(") "));
            summary.append("\n");
        }

        return summary.toString();
    }

    /**
     * Get important directories list
     */
    public List<DirectoryInfo> getImportantDirectories() {
        return directories.stream()
                .filter(DirectoryInfo::isImportant)
                .toList();
    }

    /**
     * 标记重要目录
     */
    public void markImportantDirectories() {
        if (projectType == null) return;

        for (DirectoryInfo dir : directories) {
            String dirName = dir.getName().toLowerCase();

            if (dirName.equals("src") || dirName.equals("source") ||
                    dirName.equals("test") || dirName.equals("tests") ||
                    dirName.equals("config") || dirName.equals("conf") ||
                    dirName.equals("docs") || dirName.equals("doc")) {
                dir.setImportant(true);
                continue;
            }

            switch (projectType) {
                case JAVA_MAVEN:
                case JAVA_GRADLE:
                case SPRING_BOOT:
                    if (dirName.equals("main") || dirName.equals("resources") ||
                            dirName.equals("webapp") || dirName.equals("target") ||
                            dirName.equals("build")) {
                        dir.setImportant(true);
                    }
                    break;

                case NODE_JS:
                case REACT:
                case VUE:
                case ANGULAR:
                case NEXT_JS:
                    if (dirName.equals("node_modules") || dirName.equals("public") ||
                            dirName.equals("dist") || dirName.equals("build") ||
                            dirName.equals("components") || dirName.equals("pages")) {
                        dir.setImportant(true);
                    }
                    break;

                case PYTHON:
                case DJANGO:
                case FLASK:
                case FASTAPI:
                    if (dirName.equals("venv") || dirName.equals("env") ||
                            dirName.equals("__pycache__") || dirName.equals("migrations") ||
                            dirName.equals("static") || dirName.equals("templates")) {
                        dir.setImportant(true);
                    }
                    break;
            }
        }
    }

}
