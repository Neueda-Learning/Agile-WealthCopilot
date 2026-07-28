package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.marketdata.MarketDataClient;
import com.wealthcopilot.marketdata.MarketDataClient.SymbolSearchResult;
import com.wealthcopilot.marketdata.MarketDataClientException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private MarketDataClient marketDataClient;

    @Mock
    private PriceCacheService priceCacheService;

    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService = new MarketDataService(
                marketDataClient,
                Caffeine.newBuilder().maximumSize(100).build(),
                priceCacheService
        );
    }

    @Test
    void search_filtersUnsupportedResultsAndCachesByNormalizedQuery() {
        when(marketDataClient.searchSymbols("nvidia")).thenReturn(List.of(
                new SymbolSearchResult("NVDA", "NVIDIA", "NASDAQ", "Common Stock", "USD"),
                new SymbolSearchResult("NVDA", "NVIDIA Frankfurt", "F", "Common Stock", "EUR"),
                new SymbolSearchResult("NVDA-OPT", "NVIDIA Option", "NASDAQ", "Option", "USD")
        ));

        var first = marketDataService.search("nvidia");
        var second = marketDataService.search(" NVIDIA ");

        assertEquals(1, first.size());
        assertEquals("NVDA", first.get(0).ticker());
        assertEquals(InstrumentType.STOCK, first.get(0).type());
        assertEquals(first, second);
        verify(marketDataClient, times(1)).searchSymbols("nvidia");
    }

    @Test
    void search_returnsEmptyListWhenProviderFails() {
        when(marketDataClient.searchSymbols("unknown"))
                .thenThrow(new MarketDataClientException("provider unavailable"));

        assertTrue(marketDataService.search("unknown").isEmpty());
    }
}
