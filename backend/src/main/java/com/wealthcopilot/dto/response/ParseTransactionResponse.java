package com.wealthcopilot.dto.response;

import java.util.List;

public record ParseTransactionResponse(
        TransactionDraftResponse draft,
        String confidence,
        List<String> warnings
) {
}
