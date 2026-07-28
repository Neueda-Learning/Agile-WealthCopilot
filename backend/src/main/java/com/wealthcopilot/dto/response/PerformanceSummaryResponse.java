package com.wealthcopilot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PerformanceSummaryResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal investedAmount,
        BigDecimal proceedsAmount,
        BigDecimal netInvested,
        BigDecimal realizedPnl,
        long buyCount,
        long sellCount,
        String currency
) {
}
