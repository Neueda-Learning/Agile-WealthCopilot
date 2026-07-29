package com.wealthcopilot.dto.response;

import java.time.LocalDateTime;

public record ConversationResponse(Long id, String title, LocalDateTime updatedAt) {
}
