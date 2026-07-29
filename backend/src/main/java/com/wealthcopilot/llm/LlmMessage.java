package com.wealthcopilot.llm;

import java.util.List;

/**
 * One turn of an OpenAI-compatible chat exchange. Only the fields relevant to
 * the given role are populated; use the static factories.
 */
public record LlmMessage(
        Role role,
        String content,
        List<LlmToolCall> toolCalls,
        String toolCallId,
        String toolName
) {

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    public static LlmMessage system(String content) {
        return new LlmMessage(Role.SYSTEM, content, List.of(), null, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(Role.USER, content, List.of(), null, null);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(Role.ASSISTANT, content, List.of(), null, null);
    }

    public static LlmMessage assistantToolCalls(String content, List<LlmToolCall> toolCalls) {
        return new LlmMessage(Role.ASSISTANT, content, List.copyOf(toolCalls), null, null);
    }

    public static LlmMessage toolResult(String toolCallId, String toolName, String content) {
        return new LlmMessage(Role.TOOL, content, List.of(), toolCallId, toolName);
    }
}
