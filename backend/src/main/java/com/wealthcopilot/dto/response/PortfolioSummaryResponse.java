package com.wealthcopilot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PortfolioSummaryResponse(
        BigDecimal totalValue,
        BigDecimal totalCostBasis,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPct,
        BigDecimal realizedPnl,
        BigDecimal dayChange,
        BigDecimal dayChangePct,
        String currency,
        LocalDateTime pricesAsOf,
        boolean stale
) {
}
