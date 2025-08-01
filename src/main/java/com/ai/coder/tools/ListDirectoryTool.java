package com.ai.coder.tools;

import com.ai.coder.config.AppProperties;
import com.ai.coder.model.FileInfo;
import com.ai.coder.model.ListDirectoryParams;
import com.ai.coder.model.ToolResult;
import com.ai.coder.schema.JsonSchema;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 目录列表工具
 * 列出指定目录的文件和子目录，支持递归列表
 */
@Component
public class ListDirectoryTool extends BaseTool<ListDirectoryParams> {

    private final String rootDirectory;
    private final AppProperties appProperties;

    public ListDirectoryTool(AppProperties appProperties) {
        super(
                "list_directory",
                "ListDirectory",
                "Lists files and directories in the specified path. " +
                        "Supports recursive listing and filtering. " +
                        "Shows file sizes, modification times, and types. " +
                        "Use absolute paths within the workspace directory.",
                createSchema()
        );
        this.appProperties = appProperties;
        this.rootDirectory = appProperties.getWorkspace().getRootDirectory();
    }

    private static String getWorkspaceBasePath() {
        return Paths.get(System.getProperty("user.dir"), "workspace").toString();
    }

    private static String getPathExample(String subPath) {
        return "Example: \"" + Paths.get(getWorkspaceBasePath(), subPath).toString() + "\"";
    }

    private static JsonSchema createSchema() {
        return JsonSchema.object()
                .addProperty("path", JsonSchema.string(
                        "MUST be an absolute path to the directory to list. Path must be within the workspace directory (" +
                                getWorkspaceBasePath() + "). " +
                                getPathExample("project/src") + ". " +
                                "Relative paths are NOT allowed."
                ))
                .addProperty("recursive", JsonSchema.bool(
                        "Optional: Whether to list files recursively in subdirectories. Default: false"
                ))
                .addProperty("max_depth", JsonSchema.integer(
                        "Optional: Maximum depth for recursive listing. Default: 3, Maximum: 10"
                ).minimum(1).maximum(10))
                .addProperty("show_hidden", JsonSchema.bool(
                        "Optional: Whether to show hidden files (starting with '.'). Default: false"
                ))
                .required("path");
    }

    @Override
    public String validateToolParams(ListDirectoryParams params) {
        String baseValidation = super.validateToolParams(params);
        if (baseValidation != null) {
            return baseValidation;
        }

        // 验证路径
        if (params.getPath() == null || params.getPath().trim().isEmpty()) {
            return "Directory path cannot be empty";
        }

        Path dirPath = Paths.get(params.getPath());

        // 验证是否为绝对路径
        if (!dirPath.isAbsolute()) {
            return "Directory path must be absolute: " + params.getPath();
        }

        // 验证是否在工作目录内
        if (!isWithinWorkspace(dirPath)) {
            return "Directory path must be within the workspace directory (" + rootDirectory + "): " + params.getPath();
        }

        // 验证最大深度
        if (params.getMaxDepth() != null && (params.getMaxDepth() < 1 || params.getMaxDepth() > 10)) {
            return "Max depth must be between 1 and 10";
        }

        return null;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ListDirectoryParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path dirPath = Paths.get(params.getPath());

                // 检查目录是否存在
                if (!Files.exists(dirPath)) {
                    return ToolResult.error("Directory not found: " + params.getPath());
                }

                // 检查是否为目录
                if (!Files.isDirectory(dirPath)) {
                    return ToolResult.error("Path is not a directory: " + params.getPath());
                }

                // 列出文件和目录
                List<FileInfo> fileInfos = listFiles(dirPath, params);

                // 生成输出
                String content = formatFileList(fileInfos, params);
                String relativePath = getRelativePath(dirPath);
                String displayMessage = String.format("Listed directory: %s (%d items)",
                        relativePath, fileInfos.size());

                return ToolResult.success(content, displayMessage);

            } catch (IOException e) {
                logger.error("Error listing directory: " + params.getPath(), e);
                return ToolResult.error("Error listing directory: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error listing directory: " + params.getPath(), e);
                return ToolResult.error("Unexpected error: " + e.getMessage());
            }
        });
    }

    private List<FileInfo> listFiles(Path dirPath, ListDirectoryParams params) throws IOException {
        List<FileInfo> fileInfos = new ArrayList<>();

        if (params.getRecursive() != null && params.getRecursive()) {
            int maxDepth = params.getMaxDepth() != null ? params.getMaxDepth() : 3;
            listFilesRecursive(dirPath, fileInfos, 0, maxDepth, params);
        } else {
            listFilesInDirectory(dirPath, fileInfos, params);
        }

        // 排序：目录在前，然后按名称排序
        fileInfos.sort(Comparator
                .comparing((FileInfo f) -> !f.isDirectory())
                .thenComparing(FileInfo::getName));

        return fileInfos;
    }

    private void listFilesInDirectory(Path dirPath,
                                      List<FileInfo> fileInfos,
                                      ListDirectoryParams params) throws IOException {
        try (Stream<Path> stream = Files.list(dirPath)) {
            logger.info("开始列出目录: {} (非递归)", dirPath);
            stream.forEach(path -> {
                try {
                    String fileName = path.getFileName().toString();

                    // 跳过隐藏文件（除非明确要求显示）
                    if (!params.getShowHidden() && fileName.startsWith(".")) {
                        return;
                    }

                    FileInfo fileInfo = createFileInfo(path, dirPath);
                    fileInfos.add(fileInfo);
                } catch (IOException e) {
                    logger.warn("Could not get info for file: " + path, e);
                }
            });
        }
    }

    private void listFilesRecursive(Path dirPath,
                                    List<FileInfo> fileInfos,
                                    int currentDepth,
                                    int maxDepth,
                                    ListDirectoryParams params) throws IOException {
        logger.info("开始递归列出目录: {} (当前深度: {}, 最大深度: {})", dirPath, currentDepth, maxDepth);
        if (currentDepth >= maxDepth) {
            return;
        }

        try (Stream<Path> stream = Files.list(dirPath)) {
            List<Path> paths = stream.collect(Collectors.toList());

            for (Path path : paths) {
                String fileName = path.getFileName().toString();

                // 跳过隐藏文件（除非明确要求显示）
                if (!params.getShowHidden() && fileName.startsWith(".")) {
                    continue;
                }

                try {
                    FileInfo fileInfo = createFileInfo(path, Paths.get(params.getPath()));
                    fileInfos.add(fileInfo);

                    // 如果是目录，递归列出
                    if (Files.isDirectory(path)) {
                        listFilesRecursive(path, fileInfos, currentDepth + 1, maxDepth, params);
                    }
                } catch (IOException e) {
                    logger.warn("Could not get info for file: " + path, e);
                }
            }
        }
    }

    private FileInfo createFileInfo(Path path, Path basePath) throws IOException {
        String name = path.getFileName().toString();
        boolean isDirectory = Files.isDirectory(path);
        long size = isDirectory ? 0 : Files.size(path);

        LocalDateTime lastModified = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(path).toInstant(),
                ZoneId.systemDefault()
        );

        String relativePath = basePath.relativize(path).toString();

        return new FileInfo(name, relativePath, isDirectory, size, lastModified);
    }

    private String formatFileList(List<FileInfo> fileInfos, ListDirectoryParams params) {
        if (fileInfos.isEmpty()) {
            return "Directory is empty.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Directory listing for: %s\n", getRelativePath(Paths.get(params.getPath()))));
        sb.append(String.format("Total items: %d\n\n", fileInfos.size()));

        // 表头
        sb.append(String.format("%-4s %-40s %-12s %-20s %s\n",
                "Type", "Name", "Size", "Modified", "Path"));
        sb.append("-".repeat(80)).append("\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (FileInfo fileInfo : fileInfos) {
            String type = fileInfo.isDirectory() ? "DIR" : "FILE";
            String sizeStr = fileInfo.isDirectory() ? "-" : formatFileSize(fileInfo.getSize());
            String modifiedStr = fileInfo.getLastModified().format(formatter);

            sb.append(String.format("%-4s %-40s %-12s %-20s %s\n",
                    type,
                    truncate(fileInfo.getName(), 40),
                    sizeStr,
                    modifiedStr,
                    fileInfo.getRelativePath()
            ));
        }

        return sb.toString();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private boolean isWithinWorkspace(Path dirPath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            Path normalizedPath = dirPath.normalize();
            return normalizedPath.startsWith(workspaceRoot.normalize());
        } catch (IOException e) {
            logger.warn("Could not resolve workspace path", e);
            return false;
        }
    }

    private String getRelativePath(Path dirPath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory);
            return workspaceRoot.relativize(dirPath).toString();
        } catch (Exception e) {
            return dirPath.toString();
        }
    }


}
