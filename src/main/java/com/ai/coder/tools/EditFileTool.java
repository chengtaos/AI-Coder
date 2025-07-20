package com.ai.coder.tools;

import com.ai.coder.config.AppProperties;
import com.ai.coder.model.EditFileParams;
import com.ai.coder.model.FileDiff;
import com.ai.coder.model.ToolConfirmationDetails;
import com.ai.coder.model.ToolResult;
import com.ai.coder.schema.JsonSchema;
import com.ai.coder.service.ToolExecutionLogger;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 编辑文件工具类，用于编辑文件中的内容。
 */
@Component
public class EditFileTool extends BaseTool<EditFileParams> {

    private final String rootDirectory;
    private final AppProperties appProperties;

    @Autowired
    private ToolExecutionLogger executionLogger;

    public EditFileTool(AppProperties appProperties) {
        super(
                "edit_file",
                "EditFile",
                "Edits a file by replacing specified text with new text. " +
                        "Shows a diff of the changes before applying them. " +
                        "Supports both exact string matching and line-based editing. " +
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
                .addProperty("file_path", JsonSchema.string(
                        "MUST be an absolute path to the file to edit. Path must be within the workspace directory (" +
                                getWorkspaceBasePath() + "). " +
                                getPathExample("project/src/main.java") + ". " +
                                "Relative paths are NOT allowed."
                ))
                .addProperty("old_str", JsonSchema.string(
                        "The exact string to find and replace. Must match exactly including whitespace and newlines."
                ))
                .addProperty("new_str", JsonSchema.string(
                        "The new string to replace the old string with. Can be empty to delete the old string."
                ))
                .addProperty("start_line", JsonSchema.integer(
                        "Optional: 1-based line number where the old_str starts. Helps with disambiguation."
                ).minimum(1))
                .addProperty("end_line", JsonSchema.integer(
                        "Optional: 1-based line number where the old_str ends. Must be >= start_line."
                ).minimum(1))
                .required("file_path", "old_str", "new_str");
    }

    @Override
    public String validateToolParams(EditFileParams params) {
        String baseValidation = super.validateToolParams(params);
        if (baseValidation != null) {
            return baseValidation;
        }

        // 验证路径
        if (params.getFilePath() == null || params.getFilePath().trim().isEmpty()) {
            return "File path cannot be empty";
        }

        if (params.getOldStr() == null) {
            return "Old string cannot be null";
        }

        if (params.getNewStr() == null) {
            return "New string cannot be null";
        }

        Path filePath = Paths.get(params.getFilePath());

        // Validate if it's an absolute path
        if (!filePath.isAbsolute()) {
            return "File path must be absolute: " + params.getFilePath();
        }

        // 验证是否在工作目录内
        if (!isWithinWorkspace(filePath)) {
            return "File path must be within the workspace directory (" + rootDirectory + "): " + params.getFilePath();
        }

        // 验证行号
        if (params.getStartLine() != null && params.getEndLine() != null) {
            if (params.getEndLine() < params.getStartLine()) {
                return "End line must be >= start line";
            }
        }

        return null;
    }

    /**
     * 确认执行编辑文件工具
     * @param params
     * @return
     */
    @Override
    public CompletableFuture<ToolConfirmationDetails> shouldConfirmExecute(EditFileParams params) {
        if (appProperties.getSecurity().getApprovalMode() == AppProperties.ApprovalMode.AUTO_EDIT ||
                appProperties.getSecurity().getApprovalMode() == AppProperties.ApprovalMode.YOLO) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Paths.get(params.getFilePath());

                if (!Files.exists(filePath)) {
                    return null; // 文件不存在，无法预览差异
                }

                String currentContent = Files.readString(filePath, StandardCharsets.UTF_8);
                String newContent = performEdit(currentContent, params);

                if (newContent == null) {
                    return null;
                }

                // 生成差异显示
                String diff = generateDiff(filePath.getFileName().toString(), currentContent, newContent);
                String title = "Confirm Edit: " + getRelativePath(filePath);

                return ToolConfirmationDetails.edit(title, filePath.getFileName().toString(), diff);

            } catch (IOException e) {
                logger.warn("错误编辑文件 {},{}", params.getFilePath(), e.getMessage());
                return null;
            }
        });
    }

    /**
     * 编辑文件内容
     * @param filePath
     * @param oldStr
     * @param newStr
     * @param startLine
     * @param endLine
     * @return
     */
    @Tool(name = "edit_file", description = "Edits a file by replacing specified text with new text")
    public String editFile(String filePath, String oldStr, String newStr, Integer startLine, Integer endLine) {
        long callId = executionLogger.logToolStart("edit_file", "编辑文件内容",
                String.format("文件=%s, 替换文本长度=%d->%d, 行号范围=%s-%s",
                        filePath, oldStr != null ? oldStr.length() : 0,
                        newStr != null ? newStr.length() : 0, startLine, endLine));
        long startTime = System.currentTimeMillis();

        try {
            EditFileParams params = new EditFileParams();
            params.setFilePath(filePath);
            params.setOldStr(oldStr);
            params.setNewStr(newStr);
            params.setStartLine(startLine);
            params.setEndLine(endLine);

            executionLogger.logToolStep(callId, "edit_file", "参数验证", "验证文件路径和替换内容");

            // Validate parameters
            String validation = validateToolParams(params);
            if (validation != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                executionLogger.logToolError(callId, "edit_file", "参数验证失败: " + validation, executionTime);
                return "Error: " + validation;
            }

            String editDetails = startLine != null && endLine != null ?
                    String.format("行号范围编辑: %d-%d行", startLine, endLine) : "字符串替换编辑";
            executionLogger.logFileOperation(callId, "编辑文件", filePath, editDetails);

            // Execute the tool
            ToolResult result = execute(params).join();

            long executionTime = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                executionLogger.logToolSuccess(callId, "edit_file", "文件编辑成功", executionTime);
                return result.getLlmContent();
            } else {
                executionLogger.logToolError(callId, "edit_file", result.getErrorMessage(), executionTime);
                return "Error: " + result.getErrorMessage();
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            executionLogger.logToolError(callId, "edit_file", "工具执行异常: " + e.getMessage(), executionTime);
            logger.error("Error in edit file tool", e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public CompletableFuture<ToolResult> execute(EditFileParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("开始编辑文件.............");
                Path filePath = Paths.get(params.getFilePath());

                // Check if file exists
                if (!Files.exists(filePath)) {
                    return ToolResult.error("File not found: " + params.getFilePath());
                }

                // Check if it's a file
                if (!Files.isRegularFile(filePath)) {
                    return ToolResult.error("Path is not a regular file: " + params.getFilePath());
                }

                // 读取原始内容
                String originalContent = Files.readString(filePath, StandardCharsets.UTF_8);

                // 执行编辑
                String newContent = performEdit(originalContent, params);
                if (newContent == null) {
                    return ToolResult.error("Could not find the specified text to replace in file: " + params.getFilePath());
                }

                // 创建备份
                if (shouldCreateBackup()) {
                    createBackup(filePath, originalContent);
                }

                // Write new content
                Files.writeString(filePath, newContent, StandardCharsets.UTF_8);

                // Generate differences and results
                String diff = generateDiff(filePath.getFileName().toString(), originalContent, newContent);
                String relativePath = getRelativePath(filePath);
                String successMessage = String.format("Successfully edited file: %s", params.getFilePath());

                return ToolResult.success(successMessage, new FileDiff(diff, filePath.getFileName().toString()));

            } catch (IOException e) {
                logger.error("Error editing file: " + params.getFilePath(), e);
                return ToolResult.error("Error editing file: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error editing file: " + params.getFilePath(), e);
                return ToolResult.error("Unexpected error: " + e.getMessage());
            }
        });
    }

    private String performEdit(String content, EditFileParams params) {
        if (params.getStartLine() != null && params.getEndLine() != null) {
            return performEditWithLineNumbers(content, params);
        } else {
            return performSimpleEdit(content, params);
        }
    }

    private String performSimpleEdit(String content, EditFileParams params) {
        logger.info("开始简单编辑文件");
        if (!content.contains(params.getOldStr())) {
            return null; // Cannot find string to replace
        }

        // Only replace the first match to avoid unexpected multiple replacements
        int index = content.indexOf(params.getOldStr());
        if (index == -1) {
            return null;
        }

        return content.substring(0, index) + params.getNewStr() + content.substring(index + params.getOldStr().length());
    }

    private String performEditWithLineNumbers(String content, EditFileParams params) {
        logger.info("开始行号范围编辑文件......................");
        String[] lines = content.split("\n", -1); // -1 preserve trailing empty lines

        // Validate line number range
        if (params.getStartLine() > lines.length || params.getEndLine() > lines.length) {
            return null; // Line number out of range
        }

        // Extract content from specified line range
        StringBuilder targetContent = new StringBuilder();
        for (int i = params.getStartLine() - 1; i < params.getEndLine(); i++) {
            if (i > params.getStartLine() - 1) {
                targetContent.append("\n");
            }
            targetContent.append(lines[i]);
        }

        // 检查是否匹配
        if (!targetContent.toString().equals(params.getOldStr())) {
            return null; // 指定行范围的内容与old_str不匹配
        }

        // 执行替换
        StringBuilder result = new StringBuilder();

        // 添加前面的行
        for (int i = 0; i < params.getStartLine() - 1; i++) {
            if (i > 0) result.append("\n");
            result.append(lines[i]);
        }

        // 添加新内容
        if (params.getStartLine() > 1) result.append("\n");
        result.append(params.getNewStr());

        // 添加后面的行
        for (int i = params.getEndLine(); i < lines.length; i++) {
            result.append("\n");
            result.append(lines[i]);
        }

        return result.toString();
    }

    /**
     * 生成差异
     * @param fileName
     * @param oldContent
     * @param newContent
     * @return
     */
    public String generateDiff(String fileName, String oldContent, String newContent) {
        logger.info("开始生成差异信息.............");
        // 空值检查
        if (fileName == null || oldContent == null || newContent == null) {
            logger.warn("Input parameters cannot be null");
            return "Diff generation failed: Input parameters cannot be null";
        }
        try {
            // 使用 \\R 匹配所有类型的换行符
            List<String> oldLines = Arrays.asList(oldContent.split("\\R"));
            List<String> newLines = Arrays.asList(newContent.split("\\R"));

            Patch<String> patch = DiffUtils.diff(oldLines, newLines);
            List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                    fileName + " (Original)",
                    fileName + " (Edited)",
                    oldLines,
                    patch,
                    3
            );

            return String.join("\n", unifiedDiff);
        } catch (Exception e) {
            logger.warn("Could not generate diff", e);
            return "Diff generation failed: " + e.getMessage();
        }
    }

    /**
     * 创建备份文件
     * @param filePath
     * @param content
     * @throws IOException
     */
    private void createBackup(Path filePath, String content) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = filePath.getFileName().toString() + ".backup." + timestamp;
        Path backupPath = filePath.getParent().resolve(backupFileName);

        Files.writeString(backupPath, content, StandardCharsets.UTF_8);
        logger.info("创建备份文件: {}", backupPath);
    }

    private boolean shouldCreateBackup() {
        return true; // 总是创建备份
    }

    private boolean isWithinWorkspace(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            Path normalizedPath = filePath.normalize();
            return normalizedPath.startsWith(workspaceRoot.normalize());
        } catch (IOException e) {
            logger.warn("Could not resolve workspace path", e);
            return false;
        }
    }

    private String getRelativePath(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory);
            return workspaceRoot.relativize(filePath).toString();
        } catch (Exception e) {
            return filePath.toString();
        }
    }


}
