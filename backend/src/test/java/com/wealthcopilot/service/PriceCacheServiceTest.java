package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.PriceCache;
import com.wealthcopilot.exception.MarketDataUnavailableException;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.marketdata.MarketDataProperties;
import com.wealthcopilot.repository.InstrumentRepository;
import com.wealthcopilot.repository.PriceCacheRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceCacheServiceTest {

    @Mock
    private PriceCacheRepository priceCacheRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceRefreshQueue refreshQueue;

    @Mock
    private PriceRefreshService priceRefreshService;

    private PriceCacheService priceCacheService;

    @BeforeEach
    void setUp() {
        MarketDataProperties properties = new MarketDataProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC);
        priceCacheService = new PriceCacheService(
                priceCacheRepository,
                instrumentRepository,
                refreshQueue,
                priceRefreshService,
                properties,
                clock
        );
    }

    @Test
    void getLatestQuote_returnsFreshDatabaseQuoteWithoutQueuingRefresh() {
        Instrument instrument = instrument();
        PriceCache cached = price(instrument, LocalDateTime.parse("2026-07-28T11:50:00"));
        when(priceCacheRepository.findByInstrumentTickerIgnoreCase("NVDA")).thenReturn(Optional.of(cached));

        var quote = priceCacheService.getLatestQuote("nvda").orElseThrow();

        assertFalse(quote.stale());
        verify(refreshQueue, never()).enqueue(instrument);
    }

    @Test
    void getLatestQuote_returnsStaleDatabaseQuoteAndQueuesRefresh() {
        Instrument instrument = instrument();
        PriceCache cached = price(instrument, LocalDateTime.parse("2026-07-28T11:40:00"));
        when(priceCacheRepository.findByInstrumentTickerIgnoreCase("NVDA")).thenReturn(Optional.of(cached));

        var quote = priceCacheService.getLatestQuote("NVDA").orElseThrow();

        assertTrue(quote.stale());
        verify(refreshQueue).enqueue(instrument);
    }

    @Test
    void getLatestQuote_marksQuoteStaleAtExactTtlBoundary() {
        Instrument instrument = instrument();
        PriceCache cached = price(instrument, LocalDateTime.parse("2026-07-28T11:45:00"));
        when(priceCacheRepository.findByInstrumentTickerIgnoreCase("NVDA")).thenReturn(Optional.of(cached));

        var quote = priceCacheService.getLatestQuote("NVDA").orElseThrow();

        assertTrue(quote.stale());
        verify(refreshQueue).enqueue(instrument);
    }

    @Test
    void getLatestQuote_queuesKnownInstrumentWhenDatabaseQuoteIsMissing() {
        Instrument instrument = instrument();
        when(priceCacheRepository.findByInstrumentTickerIgnoreCase("NVDA")).thenReturn(Optional.empty());
        when(instrumentRepository.findByTickerIgnoreCase("NVDA")).thenReturn(Optional.of(instrument));

        assertTrue(priceCacheService.getLatestQuote("NVDA").isEmpty());
        verify(priceRefreshService).refreshInstrument(instrument);
    }

    @Test
    void getQuote_rejectsUnknownTicker() {
        when(instrumentRepository.findByTickerIgnoreCase("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> priceCacheService.getQuote("NOPE"));
    }

    @Test
    void getQuote_queuesKnownTickerAndReturnsUnavailableWhenQuoteIsMissing() {
        Instrument instrument = instrument();
        when(instrumentRepository.findByTickerIgnoreCase("NVDA")).thenReturn(Optional.of(instrument));
        when(priceCacheRepository.findByInstrumentId(1L)).thenReturn(Optional.empty());

        assertThrows(MarketDataUnavailableException.class, () -> priceCacheService.getQuote("NVDA"));
        verify(priceRefreshService).refreshInstrument(instrument);
    }

    private Instrument instrument() {
        Instrument instrument = new Instrument();
        instrument.setId(1L);
        instrument.setTicker("NVDA");
        return instrument;
    }

    private PriceCache price(Instrument instrument, LocalDateTime fetchedAt) {
        PriceCache price = new PriceCache();
        price.setInstrument(instrument);
        price.setPrice(new BigDecimal("181.1000"));
        price.setPreviousClose(new BigDecimal("179.0000"));
        price.setAsOf(fetchedAt);
        price.setFetchedAt(fetchedAt);
        return price;
    }
}
