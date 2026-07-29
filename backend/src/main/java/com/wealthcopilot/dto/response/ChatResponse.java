package com.wealthcopilot.dto.response;

import java.util.List;

public record ChatResponse(
        Long conversationId,
        String reply,
        List<ChatToolCallResponse> toolCalls,
        TransactionDraftResponse draftTransaction
) {
}
