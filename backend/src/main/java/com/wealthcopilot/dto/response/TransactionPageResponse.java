package com.wealthcopilot.dto.response;

import java.util.List;

/**
 * Stable wire shape for the transactions table. Spring Data's Page JSON uses
 * a `number` field; the SPA contract uses `page`.
 */
public record TransactionPageResponse(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
