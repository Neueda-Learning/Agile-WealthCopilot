package com.wealthcopilot.service;

import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.PriceCache;
import com.wealthcopilot.marketdata.MarketDataClient;
import com.wealthcopilot.marketdata.MarketDataClient.MarketQuote;
import com.wealthcopilot.marketdata.MarketDataClientException;
import com.wealthcopilot.marketdata.MarketDataProperties;
import com.wealthcopilot.repository.PriceCacheRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PriceRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceRefreshService.class);

    private final PriceRefreshQueue refreshQueue;
    private final PriceCacheRepository priceCacheRepository;
    private final MarketDataClient marketDataClient;
    private final MarketDataProperties properties;
    private final Clock clock;

    public PriceRefreshService(
            PriceRefreshQueue refreshQueue,
            PriceCacheRepository priceCacheRepository,
            MarketDataClient marketDataClient,
            MarketDataProperties properties,
            Clock clock
    ) {
        this.refreshQueue = refreshQueue;
        this.priceCacheRepository = priceCacheRepository;
        this.marketDataClient = marketDataClient;
        this.properties = properties;
        this.clock = clock;
    }

    public void refreshNextBatch() {
        List<Instrument> instruments = refreshQueue.drain(properties.getCreditsPerMinute());
        if (instruments.isEmpty()) {
            return;
        }

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
                    PriceCache cached = priceCacheRepository.findByInstrumentId(instrument.getId())
                            .orElseGet(PriceCache::new);
                    cached.setInstrument(instrument);
                    cached.setPrice(quote.price());
                    cached.setPreviousClose(quote.previousClose());
                    cached.setAsOf(quote.asOf());
                    cached.setFetchedAt(fetchedAt);
                    priceCacheRepository.save(cached);
                    refreshQueue.complete(instrument);
                } catch (RuntimeException exception) {
                    refreshQueue.retry(instrument);
                    LOGGER.warn("Market data quote could not be persisted; the previous price was retained");
                }
            }
        } catch (MarketDataClientException exception) {
            instruments.forEach(refreshQueue::retry);
            LOGGER.warn("Market data refresh failed; cached prices were retained");
        }
    }
}
