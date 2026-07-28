package com.wealthcopilot.marketdata;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface MarketDataClient {

    Map<String, MarketQuote> fetchQuotes(List<String> tickers);

    List<SymbolSearchResult> searchSymbols(String query);

    record MarketQuote(
            String ticker,
            BigDecimal price,
            BigDecimal previousClose,
            LocalDateTime asOf
    ) {
    }

    record SymbolSearchResult(
            String ticker,
            String name,
            String exchange,
            String instrumentType,
            String currency
    ) {
    }
}
