package com.wealthcopilot.service;

import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.PriceCache;
import com.wealthcopilot.marketdata.MarketDataClient;
import com.wealthcopilot.marketdata.MarketDataClient.MarketQuote;
import com.wealthcopilot.marketdata.MarketDataClientException;
import com.wealthcopilot.marketdata.MarketDataProperties;
import com.wealthcopilot.repository.InstrumentRepository;
import com.wealthcopilot.repository.PriceCacheRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceRefreshService.class);

    private final PriceRefreshQueue refreshQueue;
    private final InstrumentRepository instrumentRepository;
    private final PriceCacheRepository priceCacheRepository;
    private final MarketDataClient marketDataClient;
    private final MarketDataProperties properties;
    private final Clock clock;
    /**
     * Serializes first-time cache creation for an instrument within this application instance.
     * Multiple dashboard endpoints can request the same newly-added ticker concurrently.
     */
    private final ConcurrentMap<Long, Object> instrumentLocks = new ConcurrentHashMap<>();

    public PriceRefreshService(
            PriceRefreshQueue refreshQueue,
            InstrumentRepository instrumentRepository,
            PriceCacheRepository priceCacheRepository,
            MarketDataClient marketDataClient,
            MarketDataProperties properties,
            Clock clock
    ) {
        this.refreshQueue = refreshQueue;
        this.instrumentRepository = instrumentRepository;
        this.priceCacheRepository = priceCacheRepository;
        this.marketDataClient = marketDataClient;
        this.properties = properties;
        this.clock = clock;
    }

    @CacheEvict(cacheNames = {"portfolioSummary", "portfolioHoldings"}, allEntries = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshNextBatch() {
        List<Instrument> instruments = refreshQueue.drain(properties.getCreditsPerMinute());
        if (instruments.isEmpty()) {
            return;
        }

        refreshInstruments(instruments);
    }

    /** Fetch one newly needed quote without waiting for the scheduled batch. */
    @CacheEvict(cacheNames = {"portfolioSummary", "portfolioHoldings"}, allEntries = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshInstrument(Instrument instrument) {
        if (instrument != null) {
            refreshInstruments(List.of(instrument));
        }
    }

    private void refreshInstruments(List<Instrument> instruments) {
        try {
            List<String> tickers = instruments.stream().map(Instrument::getTicker).toList();
            Map<String, MarketQuote> quotes = marketDataClient.fetchQuotes(tickers);
            LocalDateTime fetchedAt = LocalDateTime.now(clock);

            for (Instrument instrument : instruments) {
                MarketQuote quote = quotes.get(instrument.getTicker().toUpperCase(Locale.ROOT));
                if (quote == null) {
                    refreshQueue.retry(instrument);
                    continue;
                }

                try {
                    persistQuote(instrument, quote, fetchedAt);
                    refreshQueue.complete(instrument);
                } catch (RuntimeException exception) {
                    refreshQueue.retry(instrument);
                    LOGGER.warn("Market data quote could not be persisted; the previous price was retained", exception);
                }
            }
        } catch (MarketDataClientException exception) {
            instruments.forEach(refreshQueue::retry);
            LOGGER.warn("Market data refresh failed; cached prices were retained");
        }
    }

    private void persistQuote(Instrument instrument, MarketQuote quote, LocalDateTime fetchedAt) {
        Object lock = instrumentLocks.computeIfAbsent(instrument.getId(), ignored -> new Object());
        synchronized (lock) {
            // This service runs in its own transaction. Reload the instrument here so the
            // @MapsId relationship is managed by this transaction rather than by the
            // request/scheduler transaction that originally queued it.
            Instrument managedInstrument = instrumentRepository.findById(instrument.getId())
                    .orElseThrow(() -> new IllegalStateException("instrument no longer exists"));
            PriceCache cached = priceCacheRepository.findByInstrumentId(managedInstrument.getId())
                    .orElseGet(PriceCache::new);
            cached.setInstrument(managedInstrument);
            cached.setPrice(quote.price());
            cached.setPreviousClose(quote.previousClose());
            cached.setAsOf(quote.asOf());
            cached.setFetchedAt(fetchedAt);
            priceCacheRepository.save(cached);
        }
    }
}
