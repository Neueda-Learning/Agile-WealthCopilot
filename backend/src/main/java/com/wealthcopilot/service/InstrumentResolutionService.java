package com.wealthcopilot.service;

import com.wealthcopilot.dto.response.SymbolSearchResponse;
import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.repository.InstrumentRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstrumentResolutionService {

    private final InstrumentRepository instrumentRepository;
    private final MarketDataService marketDataService;

    public InstrumentResolutionService(
            InstrumentRepository instrumentRepository,
            MarketDataService marketDataService
    ) {
        this.instrumentRepository = instrumentRepository;
        this.marketDataService = marketDataService;
    }

    @Transactional
    public Instrument resolveUsdInstrument(String ticker) {
        String normalized = ticker.trim().toUpperCase(Locale.ROOT);
        Instrument existing = instrumentRepository.findByTickerIgnoreCase(normalized).orElse(null);
        if (existing != null) {
            validateUsd(existing);
            return existing;
        }

        SymbolSearchResponse match = marketDataService.search(normalized).stream()
                .filter(result -> normalized.equalsIgnoreCase(result.ticker()))
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("unknown ticker: " + normalized));

        Instrument instrument = new Instrument();
        instrument.setTicker(match.ticker());
        instrument.setName(match.name());
        instrument.setExchange(match.exchange());
        instrument.setType(match.type());
        instrument.setCurrency(match.currency());
        return instrumentRepository.save(instrument);
    }

    private void validateUsd(Instrument instrument) {
        if (!"USD".equalsIgnoreCase(instrument.getCurrency())) {
            throw new DomainValidationException("only USD instruments supported in v1");
        }
    }
}
