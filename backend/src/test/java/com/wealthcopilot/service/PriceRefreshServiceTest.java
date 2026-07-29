package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.marketdata.MarketDataClient;
import com.wealthcopilot.marketdata.MarketDataClientException;
import com.wealthcopilot.marketdata.MarketDataProperties;
import com.wealthcopilot.repository.InstrumentRepository;
import com.wealthcopilot.repository.PriceCacheRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceRefreshServiceTest {

    @Mock
    private PriceCacheRepository priceCacheRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private MarketDataClient marketDataClient;

    private PriceRefreshQueue queue;
    private PriceRefreshService refreshService;

    @BeforeEach
    void setUp() {
        queue = new PriceRefreshQueue();
        MarketDataProperties properties = new MarketDataProperties();
        properties.setCreditsPerMinute(2);
        Clock clock = Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC);
        refreshService = new PriceRefreshService(
                queue,
                instrumentRepository,
                priceCacheRepository,
                marketDataClient,
                properties,
                clock
        );
    }

    @Test
    void refreshNextBatch_respectsCreditLimitAndStoresSuccessfulQuotes() {
        queue.enqueue(instrument(1L, "AAPL"));
        queue.enqueue(instrument(2L, "MSFT"));
        queue.enqueue(instrument(3L, "NVDA"));
        when(marketDataClient.fetchQuotes(anyList())).thenAnswer(invocation -> quotes(invocation.getArgument(0)));
        when(priceCacheRepository.findByInstrumentId(any(Long.class))).thenReturn(Optional.empty());
        when(instrumentRepository.findById(any(Long.class))).thenAnswer(invocation ->
                Optional.of(instrument(invocation.getArgument(0), "AAPL")));

        refreshService.refreshNextBatch();

        verify(priceCacheRepository, times(2)).save(any());
        assertEquals(1, queue.size());
    }

    @Test
    void refreshNextBatch_requeuesEveryInstrumentWhenClientFails() {
        queue.enqueue(instrument(1L, "AAPL"));
        queue.enqueue(instrument(2L, "MSFT"));
        when(marketDataClient.fetchQuotes(anyList()))
                .thenThrow(new MarketDataClientException("provider unavailable"));

        refreshService.refreshNextBatch();

        assertEquals(2, queue.size());
    }

    private Map<String, MarketDataClient.MarketQuote> quotes(List<String> tickers) {
        Map<String, MarketDataClient.MarketQuote> quotes = new LinkedHashMap<>();
        for (String ticker : tickers) {
            quotes.put(ticker, new MarketDataClient.MarketQuote(
                    ticker,
                    new BigDecimal("100.0000"),
                    new BigDecimal("99.0000"),
                    LocalDateTime.parse("2026-07-28T11:59:00")
            ));
        }
        return quotes;
    }

    private Instrument instrument(Long id, String ticker) {
        Instrument instrument = new Instrument();
        instrument.setId(id);
        instrument.setTicker(ticker);
        return instrument;
    }
}
