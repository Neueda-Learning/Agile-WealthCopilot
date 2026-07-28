package com.wealthcopilot.dto.response;

import com.wealthcopilot.entity.InstrumentType;

public record SymbolSearchResponse(
        String ticker,
        String name,
        String exchange,
        InstrumentType type,
        String currency
) {
}
