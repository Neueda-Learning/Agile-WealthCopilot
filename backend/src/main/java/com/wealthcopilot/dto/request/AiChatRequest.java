package com.wealthcopilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record AiChatRequest(
        Long conversationId,
        @NotBlank(message = "message must not be blank")
        @Size(max = 2000, message = "message must be at most 2000 characters")
        String message,
        @Pattern(regexp = "en|zh-CN", message = "language must be en or zh-CN")
        String language
) {
}
