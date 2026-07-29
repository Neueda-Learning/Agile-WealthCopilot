package com.wealthcopilot.llm;

import java.util.List;

/**
 * Provider-agnostic chat-completion client (OpenAI-compatible wire format).
 * Default implementation targets DeepSeek; swappable per system design.
 */
public interface LlmClient {

    /**
     * @param messages ordered conversation including the system prompt
     * @param tools    function tools the model may call; empty for plain completions
     * @param jsonMode force a JSON-object response (used by the transaction parser)
     * @throws LlmClientException when the provider is unreachable or returns an error
     */
    LlmResult complete(List<LlmMessage> messages, List<LlmToolDefinition> tools, boolean jsonMode);
}
