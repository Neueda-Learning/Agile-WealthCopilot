package com.wealthcopilot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.wealthcopilot.dto.response.MarketQuoteResponse;
import com.wealthcopilot.dto.response.SymbolSearchResponse;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.marketdata.MarketDataClient;
import com.wealthcopilot.marketdata.MarketDataClient.SymbolSearchResult;
import com.wealthcopilot.marketdata.MarketDataClientException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

    private final MarketDataClient marketDataClient;
    private final Cache<String, List<SymbolSearchResult>> symbolSearchCache;
    private final PriceCacheService priceCacheService;

    public MarketDataService(
            MarketDataClient marketDataClient,
            Cache<String, List<SymbolSearchResult>> symbolSearchCache,
            PriceCacheService priceCacheService
    ) {
        this.marketDataClient = marketDataClient;
        this.symbolSearchCache = symbolSearchCache;
        this.priceCacheService = priceCacheService;
    }

    public MarketQuoteResponse getQuote(String ticker) {
        return priceCacheService.getQuote(ticker);
    }

    public List<SymbolSearchResponse> search(String query) {
        if (query == null || query.isBlank()) {
            throw new DomainValidationException("query is required");
        }
        String normalized = query.trim().toUpperCase(Locale.ROOT);
        List<SymbolSearchResult> cached = symbolSearchCache.getIfPresent(normalized);
        if (cached != null) {
            return toResponses(cached);
        }

        try {
            List<SymbolSearchResult> results = marketDataClient.searchSymbols(query.trim());
            symbolSearchCache.put(normalized, results);
            return toResponses(results);
        } catch (MarketDataClientException exception) {
            return List.of();
        }
    }

    private List<SymbolSearchResponse> toResponses(List<SymbolSearchResult> results) {
        return results.stream()
                .filter(result -> "USD".equalsIgnoreCase(result.currency()))
                .map(result -> {
                    InstrumentType type = mapType(result.instrumentType());
                    if (type == null) {
                        return null;
                    }
                    return new SymbolSearchResponse(
                            result.ticker().toUpperCase(Locale.ROOT),
                            result.name() == null ? result.ticker() : result.name(),
                            result.exchange(),
                            type,
                            result.currency().toUpperCase(Locale.ROOT)
                    );
                })
                .filter(result -> result != null)
                .toList();
    }

    private InstrumentType mapType(String providerType) {
        if (providerType == null) {
            return null;
        }
        String normalized = providerType.trim().toUpperCase(Locale.ROOT);
        if ("ETF".equals(normalized) || "EXCHANGE-TRADED FUND".equals(normalized)) {
            return InstrumentType.ETF;
        }
        if ("COMMON STOCK".equals(normalized) || "STOCK".equals(normalized)) {
            return InstrumentType.STOCK;
        }
        return null;
    }
}
