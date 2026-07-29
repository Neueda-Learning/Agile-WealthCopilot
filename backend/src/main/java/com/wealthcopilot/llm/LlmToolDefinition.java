package com.wealthcopilot.llm;

/** OpenAI-style function tool definition; the schema is a JSON Schema document. */
public record LlmToolDefinition(String name, String description, String parametersSchemaJson) {
}
