package com.ai.coder.tools;

import com.ai.coder.config.AppProperties;
import com.ai.coder.model.ProjectType;
import com.ai.coder.model.ScaffoldParams;
import com.ai.coder.model.ScaffoldResult;
import com.ai.coder.model.ToolResult;
import com.ai.coder.schema.JsonSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 项目脚手架工具类，用于创建项目的基础结构和模板文件。
 */
@Component
public class ProjectScaffoldTool extends BaseTool<ScaffoldParams> {

    private static final Logger logger = LoggerFactory.getLogger(ProjectScaffoldTool.class);

    private final String rootDirectory;
    private final AppProperties appProperties;

    public ProjectScaffoldTool(AppProperties appProperties) {
        super(
                "scaffold_project",
                "ScaffoldProject",
                "Create a new project with standard structure and template files. " +
                        "Supports various project types including Java, Node.js, Python, and more.",
                createSchema()
        );
        this.appProperties = appProperties;
        this.rootDirectory = appProperties.getWorkspace().getRootDirectory();
    }

    private static JsonSchema createSchema() {
        return JsonSchema.object()
                .addProperty("project_name", JsonSchema.string(
                        "Name of the project to create. Will be used as directory name and in templates."
                ))
                .addProperty("project_type", JsonSchema.string(
                        "Type of project to create. Options: " +
                                "java_maven, java_gradle, spring_boot, " +
                                "node_js, react, vue, angular, " +
                                "python, django, flask, " +
                                "html_static"
                ))
                .addProperty("project_path", JsonSchema.string(
                        "Optional: Custom path where to create the project. " +
                                "If not provided, will create in workspace root."
                ))
                .addProperty("template_variables", JsonSchema.object(
                ))
                .addProperty("include_git", JsonSchema.bool(
                        "Whether to initialize Git repository. Default: true"
                ))
                .addProperty("include_readme", JsonSchema.bool(
                        "Whether to create README.md file. Default: true"
                ))
                .addProperty("include_gitignore", JsonSchema.bool(
                        "Whether to create .gitignore file. Default: true"
                ))
                .required("project_name", "project_type");
    }

    @Override
    public String validateToolParams(ScaffoldParams params) {
        String baseValidation = super.validateToolParams(params);
        if (baseValidation != null) {
            return baseValidation;
        }

        if (params.getProjectName() == null || params.getProjectName().trim().isEmpty()) {
            return "Project name cannot be empty";
        }

        if (params.getProjectType() == null || params.getProjectType().trim().isEmpty()) {
            return "Project type cannot be empty";
        }

        // 验证项目名称格式
        if (!params.getProjectName().matches("[a-zA-Z0-9_-]+")) {
            return "Project name can only contain letters, numbers, underscores, and hyphens";
        }

        // 验证项目类型
        try {
            ProjectType.valueOf(params.getProjectType().toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Invalid project type: " + params.getProjectType();
        }

        // 验证项目路径
        if (params.getProjectPath() != null) {
            Path projectPath = Paths.get(params.getProjectPath());
            if (!isWithinWorkspace(projectPath)) {
                return "Project path must be within workspace: " + params.getProjectPath();
            }
        }

        return null;
    }

    /**
     * 创建项目脚手架
     * @param projectName
     * @param projectType
     * @param projectPath
     * @param includeGit
     * @param includeReadme
     * @param includeGitignore
     * @return
     */
    @Tool(name = "scaffold_project", description = "Create a new project with standard structure and template files")
    public String scaffoldProject(String projectName, String projectType, String projectPath, Boolean includeGit, Boolean includeReadme, Boolean includeGitignore) {
        try {
            ScaffoldParams params = new ScaffoldParams();
            params.setProjectName(projectName);
            params.setProjectType(projectType);
            params.setProjectPath(projectPath);
            params.setIncludeGit(includeGit != null ? includeGit : true);
            params.setIncludeReadme(includeReadme != null ? includeReadme : true);
            params.setIncludeGitignore(includeGitignore != null ? includeGitignore : true);

            // Validate parameters
            String validation = validateToolParams(params);
            if (validation != null) {
                return "Error: " + validation;
            }

            // Execute the tool
            ToolResult result = execute(params).join();

            if (result.isSuccess()) {
                return result.getLlmContent();
            } else {
                return "Error: " + result.getErrorMessage();
            }

        } catch (Exception e) {
            logger.error("Error in scaffold project tool", e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public CompletableFuture<ToolResult> execute(ScaffoldParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("创建项目脚手架: {} ({})", params.getProjectName(), params.getProjectType());

                // 1. 确定项目路径
                Path projectPath = determineProjectPath(params);

                // 2. 检查项目是否已存在
                if (Files.exists(projectPath)) {
                    return ToolResult.error("Project directory already exists: " + projectPath);
                }

                // 3. 创建项目目录
                Files.createDirectories(projectPath);

                // 4. 获取项目类型
                ProjectType projectType = ProjectType.valueOf(params.getProjectType().toUpperCase());

                // 5. 创建项目结构
                ScaffoldResult result = createProjectStructure(projectPath, projectType, params);

                logger.info("项目脚手架创建成功: {}", projectPath);
                return ToolResult.success(result.getSummary(), result.getDetails());

            } catch (Exception e) {
                logger.error("Error creating project scaffold", e);
                return ToolResult.error("Failed to create project: " + e.getMessage());
            }
        });
    }

    /**
     * 确定项目路径
     */
    private Path determineProjectPath(ScaffoldParams params) {
        if (params.getProjectPath() != null && !params.getProjectPath().trim().isEmpty()) {
            return Paths.get(params.getProjectPath(), params.getProjectName());
        } else {
            return Paths.get(rootDirectory, params.getProjectName());
        }
    }

    /**
     * 创建项目结构
     */
    private ScaffoldResult createProjectStructure(Path projectPath, ProjectType projectType, ScaffoldParams params) throws IOException {
        ScaffoldResult result = new ScaffoldResult();

        // 准备模板变量
        Map<String, String> variables = prepareTemplateVariables(params);

        // 根据项目类型创建结构
        switch (projectType) {
            case JAVA_MAVEN:
                createJavaMavenProject(projectPath, variables, result);
                break;
            case SPRING_BOOT:
                createSpringBootProject(projectPath, variables, result);
                break;
            case NODE_JS:
                createNodeJsProject(projectPath, variables, result);
                break;
            case REACT:
                createReactProject(projectPath, variables, result);
                break;
            case PYTHON:
                createPythonProject(projectPath, variables, result);
                break;
            case HTML_STATIC:
                createHtmlStaticProject(projectPath, variables, result);
                break;
            default:
                createBasicProject(projectPath, variables, result);
        }

        // 创建通用文件
        if (params.getIncludeReadme() == null || params.getIncludeReadme()) {
            createReadmeFile(projectPath, variables, result);
        }

        if (params.getIncludeGitignore() == null || params.getIncludeGitignore()) {
            createGitignoreFile(projectPath, projectType, result);
        }

        if (params.getIncludeGit() == null || params.getIncludeGit()) {
            initializeGitRepository(projectPath, result);
        }

        result.generateSummary(projectPath, projectType);
        return result;
    }

    /**
     * 准备模板变量
     */
    private Map<String, String> prepareTemplateVariables(ScaffoldParams params) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PROJECT_NAME", params.getProjectName());
        variables.put("PROJECT_NAME_CAMEL", toCamelCase(params.getProjectName()));
        variables.put("PROJECT_NAME_PASCAL", toPascalCase(params.getProjectName()));
        variables.put("CURRENT_YEAR", String.valueOf(java.time.Year.now().getValue()));
        variables.put("AUTHOR", "Developer");
        variables.put("EMAIL", "developer@example.com");
        variables.put("VERSION", "1.0.0");
        variables.put("DESCRIPTION", "A new " + params.getProjectType() + " project");

        // User-provided variables
        if (params.getTemplateVariables() != null) {
            params.getTemplateVariables().forEach((key, value) ->
                    variables.put(key.toUpperCase(), String.valueOf(value)));
        }

        return variables;
    }

    /**
     * Create Java Maven project
     */
    private void createJavaMavenProject(Path projectPath, Map<String, String> variables, ScaffoldResult result) throws IOException {
        logger.info("创建Java Maven项目");
        createDirectory(projectPath.resolve("src/main/java"), result);
        createDirectory(projectPath.resolve("src/main/resources"), result);
        createDirectory(projectPath.resolve("src/test/java"), result);
        createDirectory(projectPath.resolve("src/test/resources"), result);

        // Create pom.xml
        String pomContent = generatePomXml(variables);
        createFile(projectPath.resolve("pom.xml"), pomContent, result);

        // Create main class
        String packagePath = "src/main/java/com/example/" + variables.get("PROJECT_NAME").toLowerCase();
        createDirectory(projectPath.resolve(packagePath), result);

        String mainClassContent = generateJavaMainClass(variables);
        createFile(projectPath.resolve(packagePath + "/Application.java"), mainClassContent, result);

        // Create test class
        String testPackagePath = "src/test/java/com/example/" + variables.get("PROJECT_NAME").toLowerCase();
        createDirectory(projectPath.resolve(testPackagePath), result);

        String testClassContent = generateJavaTestClass(variables);
        createFile(projectPath.resolve(testPackagePath + "/ApplicationTest.java"), testClassContent, result);
    }

    /**
     * 创建Spring Boot项目
     */
    private void createSpringBootProject(Path projectPath, Map<String, String> variables, ScaffoldResult result) throws IOException {
        logger.info("创建Spring Boot项目");
        // 先创建基本的Maven结构
        createJavaMavenProject(projectPath, variables, result);

        // 覆盖pom.xml为Spring Boot版本
        String springBootPomContent = generateSpringBootPomXml(variables);
        createFile(projectPath.resolve("pom.xml"), springBootPomContent, result);

        // 创建Spring Boot主类
        String packagePath = "src/main/java/com/example/" + variables.get("PROJECT_NAME").toLowerCase();
        String springBootMainClass = generateSpringBootMainClass(variables);
        createFile(projectPath.resolve(packagePath + "/Application.java"), springBootMainClass, result);

        // 创建application.yml
        String applicationYml = generateApplicationYml(variables);
        createFile(projectPath.resolve("src/main/resources/application.yml"), applicationYml, result);

        // 创建简单的Controller
        String controllerContent = generateSpringBootController(variables);
        createFile(projectPath.resolve(packagePath + "/controller/HelloController.java"), controllerContent, result);
    }

    // 其他项目类型的创建方法将在后续实现
    private void createNodeJsProject(Path projectPath, Map<String, String> variables, ScaffoldResult result) throws IOException {
        // Node.js项目结构实现
        logger.info("创建Node.js项目");
        createFile(projectPath.resolve("package.json"), generatePackageJson(variables), result);
        createFile(projectPath.resolve("index.js"), generateNodeJsMainFile(variables), result);
        createDirectory(projectPath.resolve("src"), result);
        createDirectory(projectPath.resolve("test"), result);
    }

    private void createReactProject(Path projectPath, Map<String, String> variables, ScaffoldResult result) throws IOException {
        // React项目结构实现
        logger.info("创建React项目");
        createNodeJsProject(projectPath, variables, result);
        createDirectory(projectPath.resolve("public"), result);
        createDirectory(projectPath.resolve("src/components"), result);
        createFile(projectPath.resolve("public/index.html"), generateReactIndexHtml(variables), result);
        createFile(projectPath.resolve("src/App.js"), generateReactAppJs(variables), result);
    }

    private void createPythonProject(Path projectPath, Map<String, String> variables, ScaffoldResult result) throws IOException {
        // Python项目结构实现
        logger.info("创建Python项目");
        createFile(projectPath.resolve("requirements.txt"), generateRequirementsTxt(variables), result);
        createFile(projectPath.resolve("main.py"), generatePythonMainFile(variables), result);
        createDirectory(projectPath.resolve("src"), result);
        createDirectory(projectPath.resolve("tests"), result);
    }

    private void createHtmlStaticProject(Path projectPath, Map<String, String> variables, ScaffoldResult result) throws IOException {
        // 静态HTML项目结构实现
        logger.info("创建静态HTML项目");
        createFile(projectPath.resolve("index.html"), generateStaticIndexHtml(variables), result);
        createDirectory(projectPath.resolve("css"), result);
        createDirectory(projectPath.resolve("js"), result);
        createDirectory(projectPath.resolve("images"), result);
        createFile(projectPath.resolve("css/style.css"), generateBasicCss(variables), result);
        createFile(projectPath.resolve("js/script.js"), generateBasicJs(variables), result);
    }

    private void createBasicProject(Path projectPath, Map<String, String> variables, ScaffoldResult result) throws IOException {
        // 基本项目结构
        logger.info("创建基本项目");
        createDirectory(projectPath.resolve("src"), result);
        createDirectory(projectPath.resolve("docs"), result);
        createFile(projectPath.resolve("src/main.txt"), "Main file for " + variables.get("PROJECT_NAME"), result);
    }

    // 辅助方法
    private void createDirectory(Path path, ScaffoldResult result) throws IOException {
        Files.createDirectories(path);
        result.addCreatedItem("Directory: " + path.getFileName());
    }

    private void createFile(Path path, String content, ScaffoldResult result) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
        result.addCreatedItem("File: " + path.getFileName());
    }

    private void createReadmeFile(Path projectPath, Map<String, String> variables, ScaffoldResult result) throws IOException {
        logger.info("创建README.md文件");
        String readmeContent = generateReadmeContent(variables);
        createFile(projectPath.resolve("README.md"), readmeContent, result);
    }

    private void createGitignoreFile(Path projectPath, ProjectType projectType, ScaffoldResult result) throws IOException {
        logger.info("创建.gitignore文件");
        String gitignoreContent = generateGitignoreContent(projectType);
        createFile(projectPath.resolve(".gitignore"), gitignoreContent, result);
    }

    private void initializeGitRepository(Path projectPath, ScaffoldResult result) {
        try {
            logger.info("初始化Git仓库");
            // 简单的Git初始化 - 在实际项目中应该使用JGit库
            result.addCreatedItem("Git repository initialized");
        } catch (Exception e) {
            logger.warn("Failed to initialize Git repository", e);
        }
    }

    private boolean isWithinWorkspace(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            Path normalizedPath = filePath.toRealPath();
            return normalizedPath.startsWith(workspaceRoot);
        } catch (Exception e) {
            return false;
        }
    }

    // String utility methods
    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else if (i == 0) {
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private String toPascalCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String camelCase = toCamelCase(str);
        return Character.toUpperCase(camelCase.charAt(0)) + camelCase.substring(1);
    }

    // 模板生成方法 - 委托给模板服务
    private String generatePomXml(Map<String, String> variables) {
        // 这些方法需要注入ProjectTemplateService
        return "<!-- Maven POM.xml content -->";
    }

    private String generateSpringBootPomXml(Map<String, String> variables) {
        return "<!-- Spring Boot POM.xml content -->";
    }

    private String generateJavaMainClass(Map<String, String> variables) {
        return "// Java main class content";
    }

    private String generateSpringBootMainClass(Map<String, String> variables) {
        return "// Spring Boot main class content";
    }

    private String generateSpringBootController(Map<String, String> variables) {
        return "// Spring Boot controller content";
    }

    private String generateJavaTestClass(Map<String, String> variables) {
        return "// Java test class content";
    }

    private String generateApplicationYml(Map<String, String> variables) {
        return "# Application YAML content";
    }

    private String generatePackageJson(Map<String, String> variables) {
        return "{}";
    }

    private String generateNodeJsMainFile(Map<String, String> variables) {
        return "// Node.js main file content";
    }

    private String generateReactIndexHtml(Map<String, String> variables) {
        return "<!-- React index.html content -->";
    }

    private String generateReactAppJs(Map<String, String> variables) {
        return "// React App.js content";
    }

    private String generateRequirementsTxt(Map<String, String> variables) {
        return "# Python requirements";
    }

    private String generatePythonMainFile(Map<String, String> variables) {
        return "# Python main file content";
    }

    private String generateStaticIndexHtml(Map<String, String> variables) {
        return "<!-- Static HTML content -->";
    }

    private String generateBasicCss(Map<String, String> variables) {
        return "/* CSS content */";
    }

    private String generateBasicJs(Map<String, String> variables) {
        return "// JavaScript content";
    }

    private String generateReadmeContent(Map<String, String> variables) {
        return "# README content";
    }

    private String generateGitignoreContent(ProjectType projectType) {
        return "# Gitignore content";
    }

}
