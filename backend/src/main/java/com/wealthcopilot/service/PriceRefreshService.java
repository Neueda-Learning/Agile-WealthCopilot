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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final PriceRefreshBudget refreshBudget;
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
            PriceRefreshBudget refreshBudget,
            InstrumentRepository instrumentRepository,
            PriceCacheRepository priceCacheRepository,
            MarketDataClient marketDataClient,
            MarketDataProperties properties,
            Clock clock
    ) {
        this.refreshQueue = refreshQueue;
        this.refreshBudget = refreshBudget;
        this.instrumentRepository = instrumentRepository;
        this.priceCacheRepository = priceCacheRepository;
        this.marketDataClient = marketDataClient;
        this.properties = properties;
        this.clock = clock;
    }

    @CacheEvict(cacheNames = {"portfolioSummary", "portfolioHoldings"}, allEntries = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshNextBatch() {
        int granted = refreshBudget.tryConsume(properties.getCreditsPerMinute());
        if (granted == 0) {
            return;
        }

        List<Instrument> instruments = refreshQueue.drain(granted);
        refreshBudget.refund(granted - instruments.size());
        if (instruments.isEmpty()) {
            return;
        }

        refreshInstruments(instruments, true);
    }

    /** Fetch one newly needed quote without waiting for the scheduled batch. */
    @CacheEvict(cacheNames = {"portfolioSummary", "portfolioHoldings"}, allEntries = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshInstrument(Instrument instrument) {
        if (instrument != null) {
            refreshInstruments(List.of(instrument), false);
        }
    }

    /**
     * User-triggered refresh. Ignores the cache TTL entirely and fetches from the
     * provider in one blocking call, so the response carries prices that were
     * live at the moment the button was pressed.
     *
     * <p>The provider bills one credit per symbol and rejects the entire request
     * once the per-minute allowance is gone, so a portfolio larger than the
     * allowance is trimmed to what the plan permits: the stalest symbols are
     * fetched now, and the remainder are handed to the background queue and
     * reported back as {@code queuedTickers}. Fetching all of them eagerly is
     * what produced a 429 and refreshed nothing at all.
     */
    @CacheEvict(cacheNames = {"portfolioSummary", "portfolioHoldings"}, allEntries = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefreshResult refreshInstrumentsNow(List<Instrument> instruments) {
        Map<Long, Instrument> unique = new LinkedHashMap<>();
        if (instruments != null) {
            for (Instrument instrument : instruments) {
                if (instrument != null && instrument.getId() != null) {
                    unique.putIfAbsent(instrument.getId(), instrument);
                }
            }
        }
        if (unique.isEmpty()) {
            return new RefreshResult(0, 0, List.of(), List.of(), 0L, LocalDateTime.now(clock));
        }

        List<Instrument> ordered = stalestFirst(List.copyOf(unique.values()));
        int granted = refreshBudget.tryConsume(ordered.size());
        List<Instrument> attempted = ordered.subList(0, granted);
        List<Instrument> deferred = ordered.subList(granted, ordered.size());

        deferred.forEach(refreshQueue::enqueue);
        List<String> queuedTickers = deferred.stream().map(Instrument::getTicker).toList();
        long retryAfterSeconds = deferred.isEmpty()
                ? 0L
                : Math.max(1L, refreshBudget.timeUntilReset().toSeconds());
        if (!deferred.isEmpty()) {
            LOGGER.info(
                    "User refresh trimmed to {} of {} symbols by the market-data credit budget; "
                            + "the remainder was queued for the background refresh",
                    granted,
                    ordered.size());
        }

        RefreshResult fetched = refreshInstruments(List.copyOf(attempted), false);
        return new RefreshResult(
                ordered.size(),
                fetched.refreshed(),
                fetched.failedTickers(),
                queuedTickers,
                retryAfterSeconds,
                fetched.completedAt());
    }

    /**
     * Orders instruments so the least recently fetched are refreshed first. With a
     * budget smaller than the portfolio, this lets successive refreshes work
     * through every holding instead of repeatedly re-fetching the same few.
     */
    private List<Instrument> stalestFirst(List<Instrument> instruments) {
        Map<Long, LocalDateTime> fetchedAt = new HashMap<>();
        for (PriceCache cached : priceCacheRepository.findAllByInstrumentIdIn(
                instruments.stream().map(Instrument::getId).toList())) {
            fetchedAt.put(cached.getInstrumentId(), cached.getFetchedAt());
        }
        List<Instrument> ordered = new ArrayList<>(instruments);
        // Never fetched sorts first: those holdings have no price to show at all.
        ordered.sort(Comparator.comparing(
                instrument -> fetchedAt.get(instrument.getId()),
                Comparator.nullsFirst(Comparator.naturalOrder())));
        return ordered;
    }

    private RefreshResult refreshInstruments(List<Instrument> instruments, boolean schedulerOwned) {
        LocalDateTime completedAt = LocalDateTime.now(clock);
        if (instruments.isEmpty()) {
            return new RefreshResult(0, 0, List.of(), List.of(), 0L, completedAt);
        }

        List<String> failedTickers = new ArrayList<>();
        int refreshed = 0;
        try {
            List<String> tickers = instruments.stream().map(Instrument::getTicker).toList();
            Map<String, MarketQuote> quotes = marketDataClient.fetchQuotes(tickers);
            LocalDateTime fetchedAt = completedAt;

            for (Instrument instrument : instruments) {
                MarketQuote quote = quotes.get(instrument.getTicker().toUpperCase(Locale.ROOT));
                if (quote == null) {
                    failedTickers.add(instrument.getTicker());
                    requeue(instrument, schedulerOwned);
                    continue;
                }

                try {
                    persistQuote(instrument, quote, fetchedAt);
                    refreshed++;
                    if (schedulerOwned) {
                        refreshQueue.complete(instrument);
                    } else {
                        refreshQueue.removeQueued(instrument);
                    }
                } catch (RuntimeException exception) {
                    failedTickers.add(instrument.getTicker());
                    requeue(instrument, schedulerOwned);
                    LOGGER.warn("Market data quote could not be persisted; the previous price was retained", exception);
                }
            }
        } catch (MarketDataClientException exception) {
            instruments.forEach(instrument -> requeue(instrument, schedulerOwned));
            instruments.stream().map(Instrument::getTicker).forEach(failedTickers::add);
            LOGGER.warn("Market data refresh failed; cached prices were retained");
        }
        return new RefreshResult(
                instruments.size(),
                refreshed,
                List.copyOf(failedTickers),
                List.of(),
                0L,
                completedAt);
    }

    private void requeue(Instrument instrument, boolean schedulerOwned) {
        if (schedulerOwned) {
            refreshQueue.retry(instrument);
        } else {
            refreshQueue.enqueue(instrument);
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

    /**
     * @param requested          symbols the caller asked for
     * @param refreshed          symbols whose price was fetched and persisted
     * @param failedTickers      symbols attempted against the provider that came back empty
     * @param queuedTickers      symbols deferred to the background queue by the credit budget
     * @param retryAfterSeconds  seconds until more credits are available, 0 when none were deferred
     */
    public record RefreshResult(
            int requested,
            int refreshed,
            List<String> failedTickers,
            List<String> queuedTickers,
            long retryAfterSeconds,
            LocalDateTime completedAt
    ) {
    }
}
