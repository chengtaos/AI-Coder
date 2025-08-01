package com.ai.coder.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 该组件用于对 JSON 数据进行 Schema 验证，支持标准的 JSON Schema 验证和简单类型验证。
 */
@Component
public class SchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(SchemaValidator.class);

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public SchemaValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    /**
     * 根据给定的 JSON Schema 验证数据。
     *
     * @param schema 自定义的 JSON Schema 对象，描述数据应遵循的规则。
     * @param data   待验证的数据对象。
     * @return 若验证通过返回 null，否则返回包含错误信息的字符串。
     */
    public String validate(JsonSchema schema, Object data) {
        try {
            // Convert custom JsonSchema to standard JSON Schema string
            String schemaJson = objectMapper.writeValueAsString(schema);
            logger.debug("Schema JSON: {}", schemaJson);

            // Create JSON Schema validator
            com.networknt.schema.JsonSchema jsonSchema = schemaFactory.getSchema(schemaJson);

            // Convert data to JsonNode
            String dataJson = objectMapper.writeValueAsString(data);
            JsonNode dataNode = objectMapper.readTree(dataJson);
            logger.debug("Data JSON: {}", dataJson);

            // Execute validation
            Set<ValidationMessage> errors = jsonSchema.validate(dataNode);

            if (errors.isEmpty()) {
                logger.debug("Schema validation passed");
                return null; // Validation passed
            } else {
                String errorMessage = errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining("; "));
                logger.warn("Schema validation failed: {}", errorMessage);
                return errorMessage;
            }

        } catch (Exception e) {
            String errorMessage = "Schema validation error: " + e.getMessage();
            logger.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 简单类型验证方法，用于验证数据是否符合自定义 JSON Schema 中定义的简单类型。
     *
     * @param schema 自定义的 JSON Schema 对象，描述数据应遵循的规则。
     * @param data   待验证的数据对象。
     * @return 若验证通过返回 null，否则返回包含错误信息的字符串。
     */
    public String validateSimple(JsonSchema schema, Object data) {
        if (schema == null || data == null) {
            return "Schema or data is null";
        }

        // Basic type checking
        String expectedType = schema.getType();
        if (expectedType != null) {
            String actualType = getDataType(data);
            if (!isTypeCompatible(expectedType, actualType)) {
                return String.format("Type mismatch: expected %s, got %s", expectedType, actualType);
            }
        }

        // Required field checking (only for object type)
        if ("object".equals(expectedType) && schema.getRequiredFields() != null) {
            if (!(data instanceof java.util.Map)) {
                return "Expected object type for required field validation";
            }

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;

            for (String requiredField : schema.getRequiredFields()) {
                if (!dataMap.containsKey(requiredField) || dataMap.get(requiredField) == null) {
                    return "Missing required field: " + requiredField;
                }
            }
        }

        return null; // Validation passed
    }

    private String getDataType(Object data) {
        if (data == null) return "null";
        if (data instanceof String) return "string";
        if (data instanceof Integer || data instanceof Long) return "integer";
        if (data instanceof Number) return "number";
        if (data instanceof Boolean) return "boolean";
        if (data instanceof java.util.List) return "array";
        if (data instanceof java.util.Map) return "object";
        return "unknown";
    }

    private boolean isTypeCompatible(String expectedType, String actualType) {
        if (expectedType.equals(actualType)) {
            return true;
        }

        // Number type compatibility
        if ("number".equals(expectedType) && "integer".equals(actualType)) {
            return true;
        }

        return false;
    }
}
