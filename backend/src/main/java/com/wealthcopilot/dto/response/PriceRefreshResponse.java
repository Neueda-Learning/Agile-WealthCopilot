package com.wealthcopilot.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param requested         held symbols the refresh covered
 * @param refreshed         symbols re-priced from the provider during this request
 * @param failedTickers     symbols the provider was asked for but did not return
 * @param queuedTickers     symbols deferred to the background refresh because the
 *                          market-data plan's per-minute credit allowance ran out
 * @param retryAfterSeconds seconds until more credits are available; 0 when nothing was deferred
 */
public record PriceRefreshResponse(
        int requested,
        int refreshed,
        List<String> failedTickers,
        List<String> queuedTickers,
        long retryAfterSeconds,
        LocalDateTime completedAt
) {
}
