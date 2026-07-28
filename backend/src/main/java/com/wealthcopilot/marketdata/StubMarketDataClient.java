package com.wealthcopilot.marketdata;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("stub")
public class StubMarketDataClient implements MarketDataClient {

    private static final Map<String, BigDecimal> PRICES = Map.of(
            "AAPL", new BigDecimal("215.4000"),
            "MSFT", new BigDecimal("425.7500"),
            "NVDA", new BigDecimal("181.1000")
    );

    private static final List<SymbolSearchResult> SYMBOLS = List.of(
            new SymbolSearchResult("AAPL", "Apple Inc", "NASDAQ", "Common Stock", "USD"),
            new SymbolSearchResult("MSFT", "Microsoft Corporation", "NASDAQ", "Common Stock", "USD"),
            new SymbolSearchResult("NVDA", "NVIDIA Corporation", "NASDAQ", "Common Stock", "USD")
    );

    private final Clock clock;

    public StubMarketDataClient(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Map<String, MarketQuote> fetchQuotes(List<String> tickers) {
        Map<String, MarketQuote> quotes = new LinkedHashMap<>();
        for (String tickerValue : tickers) {
            String ticker = tickerValue.toUpperCase(Locale.ROOT);
            BigDecimal price = PRICES.get(ticker);
            if (price != null) {
                quotes.put(ticker, new MarketQuote(
                        ticker,
                        price,
                        price.subtract(new BigDecimal("1.0000")),
                        LocalDateTime.now(clock)
                ));
            }
        }
        return quotes;
    }

    @Override
    public List<SymbolSearchResult> searchSymbols(String query) {
        String normalized = query.toUpperCase(Locale.ROOT);
        return SYMBOLS.stream()
                .filter(symbol -> symbol.ticker().contains(normalized)
                        || symbol.name().toUpperCase(Locale.ROOT).contains(normalized))
                .toList();
    }
}
