package com.ai.coder.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JsonSchema 类用于表示 JSON Schema 规范中的模式定义。
 * 该类提供了创建和配置不同类型 JSON Schema 的方法，
 * 包括对象、字符串、数字、整数、布尔值和数组类型。
 * 使用 Jackson 注解确保在序列化时忽略值为 null 的字段。
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonSchema {

    // Getters and Setters
    private String type;
    private String description;
    private String pattern;
    private Number minimum;
    private Number maximum;
    private List<Object> enumValues;

    @JsonProperty("properties")
    private Map<String, JsonSchema> properties;

    @JsonProperty("required")
    private List<String> requiredFields;

    @JsonProperty("items")
    private JsonSchema items;

    // Constructor
    public JsonSchema() {
    }

    // Static factory methods
    public static JsonSchema object() {
        JsonSchema schema = new JsonSchema();
        schema.type = "object";
        schema.properties = new HashMap<>();
        return schema;
    }

    public static JsonSchema string(String description) {
        JsonSchema schema = new JsonSchema();
        schema.type = "string";
        schema.description = description;
        return schema;
    }

    public static JsonSchema number(String description) {
        JsonSchema schema = new JsonSchema();
        schema.type = "number";
        schema.description = description;
        return schema;
    }

    public static JsonSchema integer(String description) {
        JsonSchema schema = new JsonSchema();
        schema.type = "integer";
        schema.description = description;
        return schema;
    }

    public static JsonSchema bool(String description) {
        JsonSchema schema = new JsonSchema();
        schema.type = "boolean";
        schema.description = description;
        return schema;
    }

    public static JsonSchema array(JsonSchema items) {
        JsonSchema schema = new JsonSchema();
        schema.type = "array";
        schema.items = items;
        return schema;
    }

    public static JsonSchema array(String description, JsonSchema items) {
        JsonSchema schema = new JsonSchema();
        schema.type = "array";
        schema.description = description;
        schema.items = items;
        return schema;
    }

    // Fluent methods
    public JsonSchema addProperty(String name, JsonSchema property) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(name, property);
        return this;
    }

    public JsonSchema required(String... fields) {
        this.requiredFields = Arrays.asList(fields);
        return this;
    }

    public JsonSchema pattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public JsonSchema minimum(Number minimum) {
        this.minimum = minimum;
        return this;
    }

    public JsonSchema maximum(Number maximum) {
        this.maximum = maximum;
        return this;
    }

    public JsonSchema enumValues(Object... values) {
        this.enumValues = Arrays.asList(values);
        return this;
    }

}
