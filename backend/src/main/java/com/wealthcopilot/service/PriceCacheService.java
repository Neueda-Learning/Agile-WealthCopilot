package com.wealthcopilot.service;

import com.wealthcopilot.dto.response.MarketQuoteResponse;
import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.PriceCache;
import com.wealthcopilot.exception.MarketDataUnavailableException;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.marketdata.MarketDataProperties;
import com.wealthcopilot.repository.InstrumentRepository;
import com.wealthcopilot.repository.PriceCacheRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceCacheService implements PriceQuoteProvider {

    private final PriceCacheRepository priceCacheRepository;
    private final InstrumentRepository instrumentRepository;
    private final PriceRefreshQueue refreshQueue;
    private final PriceRefreshService priceRefreshService;
    private final MarketDataProperties properties;
    private final Clock clock;

    public PriceCacheService(
            PriceCacheRepository priceCacheRepository,
            InstrumentRepository instrumentRepository,
            PriceRefreshQueue refreshQueue,
            PriceRefreshService priceRefreshService,
            MarketDataProperties properties,
            Clock clock
    ) {
        this.priceCacheRepository = priceCacheRepository;
        this.instrumentRepository = instrumentRepository;
        this.refreshQueue = refreshQueue;
        this.priceRefreshService = priceRefreshService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<QuoteSnapshot> getLatestQuote(String ticker) {
        String normalized = normalize(ticker);
        Optional<PriceCache> cached = priceCacheRepository.findByInstrumentTickerIgnoreCase(normalized);
        if (cached.isEmpty()) {
            Instrument instrument = instrumentRepository.findByTickerIgnoreCase(normalized).orElse(null);
            if (instrument == null) {
                return Optional.empty();
            }
            refreshNow(instrument);
            cached = priceCacheRepository.findByInstrumentTickerIgnoreCase(normalized);
            if (cached.isEmpty()) {
                return Optional.empty();
            }
        }

        PriceCache price = cached.get();
        boolean stale = isStale(price);
        if (stale) {
            refreshQueue.enqueue(price.getInstrument());
        }
        return Optional.of(new QuoteSnapshot(
                price.getPrice(),
                price.getPreviousClose(),
                price.getAsOf(),
                stale
        ));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MarketQuoteResponse getQuote(String ticker) {
        String normalized = normalize(ticker);
        Instrument instrument = instrumentRepository.findByTickerIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("unknown ticker: " + normalized));
        PriceCache cached = priceCacheRepository.findByInstrumentId(instrument.getId()).orElse(null);
        if (cached == null) {
            refreshNow(instrument);
            cached = priceCacheRepository.findByInstrumentId(instrument.getId())
                    .orElseThrow(() -> new MarketDataUnavailableException("market quote is not available yet"));
        }

        boolean stale = isStale(cached);
        if (stale) {
            refreshQueue.enqueue(instrument);
        }
        return new MarketQuoteResponse(
                instrument.getTicker(),
                cached.getPrice(),
                cached.getPreviousClose(),
                cached.getAsOf(),
                stale
        );
    }

    private boolean isStale(PriceCache price) {
        LocalDateTime staleBefore = LocalDateTime.now(clock).minus(properties.getCacheTtl());
        return !price.getFetchedAt().isAfter(staleBefore);
    }

    private void refreshNow(Instrument instrument) {
        priceRefreshService.refreshInstrument(instrument);
    }

    private String normalize(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new ResourceNotFoundException("unknown ticker");
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
