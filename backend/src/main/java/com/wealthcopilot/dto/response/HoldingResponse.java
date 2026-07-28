package com.wealthcopilot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HoldingResponse(
        String ticker,
        String name,
        String type,
        BigDecimal quantity,
        BigDecimal avgCost,
        BigDecimal costBasis,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPct,
        BigDecimal dayChangePct,
        BigDecimal weightPct,
        LocalDateTime priceAsOf,
        boolean stale
) {
}
