package com.ai.coder.tools;

import com.ai.coder.schema.JsonSchema;
import com.ai.coder.schema.SchemaValidator;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 基础工具类，定义了工具的基本属性和方法。
 *
 * @param <P> 工具参数的类型
 */
@Data
public abstract class BaseTool<P> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // Getters
    protected final String name;
    protected final String displayName;
    protected final String description;
    protected final JsonSchema parameterSchema;
    protected final boolean isOutputMarkdown;
    protected final boolean canUpdateOutput;

    protected SchemaValidator schemaValidator;

    public BaseTool(String name, String displayName, String description, JsonSchema parameterSchema) {
        this(name, displayName, description, parameterSchema, true, false);
    }

    public BaseTool(String name, String displayName, String description, JsonSchema parameterSchema,
                    boolean isOutputMarkdown, boolean canUpdateOutput) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.parameterSchema = parameterSchema;
        this.isOutputMarkdown = isOutputMarkdown;
        this.canUpdateOutput = canUpdateOutput;
    }


    /**
     * 验证工具参数是否符合预期
     *
     * @param params 工具参数对象
     * @return 验证结果，若验证通过返回 null，否则返回包含错误信息的字符串。
     */
    public String validateToolParams(P params) {
        if (schemaValidator == null || parameterSchema == null) {
            logger.warn("Schema validator or parameter schema is null, skipping validation");
            return null;
        }

        try {
            return schemaValidator.validate(parameterSchema, params);
        } catch (Exception e) {
            logger.error("Parameter validation failed", e);
            return "Parameter validation error: " + e.getMessage();
        }
    }


    public CompletableFuture<ToolConfirmationDetails> shouldConfirmExecute(P params) {
        return CompletableFuture.completedFuture(null); // Default no confirmation needed
    }


    public abstract CompletableFuture<ToolResult> execute(P params);


    public String getDescription(P params) {
        return description;
    }

    public boolean canUpdateOutput() {
        return canUpdateOutput;
    }

    @Override
    public String toString() {
        return String.format("Tool{name='%s', displayName='%s'}", name, displayName);
    }
}
