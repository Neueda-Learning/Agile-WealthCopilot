package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.PriceCache;
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
    private PriceRefreshBudget budget;
    private PriceRefreshService refreshService;

    @BeforeEach
    void setUp() {
        queue = new PriceRefreshQueue();
        MarketDataProperties properties = new MarketDataProperties();
        properties.setCreditsPerMinute(2);
        Clock clock = Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC);
        budget = new PriceRefreshBudget(properties, clock);
        refreshService = new PriceRefreshService(
                queue,
                budget,
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

    @Test
    void refreshInstrumentsNow_refreshesEveryRequestedInstrumentImmediately() {
        List<Instrument> instruments = List.of(instrument(1L, "AAPL"), instrument(2L, "MSFT"));
        when(marketDataClient.fetchQuotes(List.of("AAPL", "MSFT")))
                .thenReturn(quotes(List.of("AAPL", "MSFT")));
        stubPersistence(instruments);

        PriceRefreshService.RefreshResult result =
                refreshService.refreshInstrumentsNow(instruments);

        assertEquals(2, result.requested());
        assertEquals(2, result.refreshed());
        assertEquals(List.of(), result.failedTickers());
        assertEquals(List.of(), result.queuedTickers());
        assertEquals(0L, result.retryAfterSeconds());
        verify(marketDataClient).fetchQuotes(List.of("AAPL", "MSFT"));
        verify(priceCacheRepository, times(2)).save(any());
    }

    @Test
    void refreshInstrumentsNow_trimsToTheCreditBudgetInsteadOfFailingTheWholeRequest() {
        List<Instrument> instruments = List.of(
                instrument(1L, "AAPL"),
                instrument(2L, "MSFT"),
                instrument(3L, "NVDA"));
        when(marketDataClient.fetchQuotes(List.of("AAPL", "MSFT")))
                .thenReturn(quotes(List.of("AAPL", "MSFT")));
        stubPersistence(instruments);

        PriceRefreshService.RefreshResult result =
                refreshService.refreshInstrumentsNow(instruments);

        // The provider bills a credit per symbol and rejects the whole call once the
        // per-minute allowance is gone, so asking for all three would refresh none.
        assertEquals(3, result.requested());
        assertEquals(2, result.refreshed());
        assertEquals(List.of(), result.failedTickers());
        assertEquals(List.of("NVDA"), result.queuedTickers());
        assertTrue(result.retryAfterSeconds() > 0);
        assertEquals(1, queue.size());
        verify(marketDataClient).fetchQuotes(List.of("AAPL", "MSFT"));
    }

    @Test
    void refreshInstrumentsNow_spendsTheBudgetOnTheLeastRecentlyFetchedSymbols() {
        List<Instrument> instruments = List.of(
                instrument(1L, "AAPL"),
                instrument(2L, "MSFT"),
                instrument(3L, "NVDA"));
        // Built before the stubbing call: cachedAt() stubs its own mock.
        List<PriceCache> cached = List.of(
                cachedAt(1L, "2026-07-28T11:59:00"),
                cachedAt(2L, "2026-07-28T10:00:00"),
                cachedAt(3L, "2026-07-28T09:00:00"));
        when(priceCacheRepository.findAllByInstrumentIdIn(List.of(1L, 2L, 3L))).thenReturn(cached);
        when(marketDataClient.fetchQuotes(List.of("NVDA", "MSFT")))
                .thenReturn(quotes(List.of("NVDA", "MSFT")));
        stubPersistence(instruments);

        PriceRefreshService.RefreshResult result =
                refreshService.refreshInstrumentsNow(instruments);

        assertEquals(2, result.refreshed());
        assertEquals(List.of("AAPL"), result.queuedTickers());
        verify(marketDataClient).fetchQuotes(List.of("NVDA", "MSFT"));
    }

    @Test
    void refreshInstrumentsNow_keepsCachedPricesWhenTheProviderIsUnavailable() {
        List<Instrument> instruments = List.of(instrument(1L, "AAPL"), instrument(2L, "MSFT"));
        when(marketDataClient.fetchQuotes(List.of("AAPL", "MSFT")))
                .thenThrow(new MarketDataClientException("provider unavailable"));

        PriceRefreshService.RefreshResult result =
                refreshService.refreshInstrumentsNow(instruments);

        assertEquals(2, result.requested());
        assertEquals(0, result.refreshed());
        assertEquals(List.of("AAPL", "MSFT"), result.failedTickers());
        verify(priceCacheRepository, never()).save(any());
        assertEquals(2, queue.size());
    }

    private void stubPersistence(List<Instrument> instruments) {
        when(priceCacheRepository.findByInstrumentId(any(Long.class))).thenReturn(Optional.empty());
        when(instrumentRepository.findById(any(Long.class))).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Optional.of(instruments.get(id.intValue() - 1));
        });
    }

    private PriceCache cachedAt(Long instrumentId, String fetchedAt) {
        PriceCache cached = mock(PriceCache.class);
        when(cached.getInstrumentId()).thenReturn(instrumentId);
        when(cached.getFetchedAt()).thenReturn(LocalDateTime.parse(fetchedAt));
        return cached;
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
