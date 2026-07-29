package com.wealthcopilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record AiParseRequest(
        @NotBlank(message = "text must not be blank")
        @Size(max = 1000, message = "text must be at most 1000 characters")
        String text,
        @Pattern(regexp = "en|zh-CN", message = "language must be en or zh-CN")
        String language
) {
}
