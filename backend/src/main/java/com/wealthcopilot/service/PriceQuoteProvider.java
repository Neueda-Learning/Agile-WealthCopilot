package com.wealthcopilot.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PriceQuoteProvider {

    Optional<QuoteSnapshot> getLatestQuote(String ticker);

    record QuoteSnapshot(
            BigDecimal price,
            BigDecimal previousClose,
            LocalDateTime asOf,
            boolean stale
    ) {
    }
}
