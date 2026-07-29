package com.wealthcopilot.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PriceRefreshResponse(
        int requested,
        int refreshed,
        List<String> failedTickers,
        LocalDateTime completedAt
) {
}
