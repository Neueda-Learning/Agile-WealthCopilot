package com.wealthcopilot.dto.response;

import com.wealthcopilot.entity.ChatRole;
import java.time.LocalDateTime;

public record ChatMessageResponse(ChatRole role, String content, LocalDateTime createdAt) {
}
