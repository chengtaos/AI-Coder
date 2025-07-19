package com.ai.coder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Paths;
import java.util.List;

/**
 * 应用配置属性
 */
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private Workspace workspace = new Workspace();
    private Security security = new Security();
    private Tools tools = new Tools();
    private Browser browser = new Browser();

    /**
     * 工作空间配置
     */
    @Data
    public static class Workspace {
        // 使用 Paths.get() 和 File.separator 实现跨平台兼容
        private String rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
        private long maxFileSize = 10485760L; // 10MB
        private List<String> allowedExtensions = List.of(
                ".txt", ".md", ".java", ".js", ".ts", ".json", ".xml",
                ".yml", ".yaml", ".properties", ".html", ".css", ".sql"
        );

        public void setRootDirectory(String rootDirectory) {
            // 确保设置的路径也是跨平台兼容的
            this.rootDirectory = Paths.get(rootDirectory).toString();
        }

    }

    /**
     * 安全配置
     */
    @Data
    public static class Security {
        private ApprovalMode approvalMode = ApprovalMode.DEFAULT;
        private List<String> dangerousCommands = List.of("rm", "del", "format", "fdisk", "mkfs");

    }

    /**
     * 工具配置
     */
    @Data
    public static class Tools {
        private ToolConfig readFile = new ToolConfig(true);
        private ToolConfig writeFile = new ToolConfig(true);
        private ToolConfig editFile = new ToolConfig(true);
        private ToolConfig listDirectory = new ToolConfig(true);
        private ToolConfig shell = new ToolConfig(true);

    }

    /**
     * 工具配置
     */
    @Data
    public static class ToolConfig {
        private boolean enabled;

        public ToolConfig() {
        }

        public ToolConfig(boolean enabled) {
            this.enabled = enabled;
        }

    }

    /**
     * 浏览器配置
     */
    @Data
    public static class Browser {
        private boolean autoOpen = true;
        private String url = "http://localhost:8080";
        private int delaySeconds = 2;

    }

    /**
     * 审批模式
     */
    public enum ApprovalMode {
        DEFAULT,    // 默认模式，危险操作需要确认
        AUTO_EDIT,  // 自动编辑模式，文件编辑不需要确认
        YOLO        // 完全自动模式，所有操作都不需要确认
    }
}
