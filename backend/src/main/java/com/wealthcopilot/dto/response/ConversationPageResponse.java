package com.wealthcopilot.dto.response;

import java.util.List;

/** Same pagination envelope as TransactionPageResponse (SPA contract). */
public record ConversationPageResponse(
        List<ConversationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
