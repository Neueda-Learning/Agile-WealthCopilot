package com.wealthcopilot.llm;

import java.util.List;

/** The assistant turn returned by the model: free text, tool requests, or both. */
public record LlmResult(String content, List<LlmToolCall> toolCalls) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
