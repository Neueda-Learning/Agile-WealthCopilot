package com.wealthcopilot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketQuoteResponse(
        String ticker,
        BigDecimal price,
        BigDecimal previousClose,
        LocalDateTime asOf,
        boolean stale
) {
}
