package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.dto.response.SymbolSearchResponse;
import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.repository.InstrumentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstrumentResolutionServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private MarketDataService marketDataService;

    private InstrumentResolutionService resolutionService;

    @BeforeEach
    void setUp() {
        resolutionService = new InstrumentResolutionService(instrumentRepository, marketDataService);
    }

    @Test
    void resolveUsdInstrument_savesExactProviderMatchWhenTickerIsNew() {
        when(instrumentRepository.findByTickerIgnoreCase("NVDA")).thenReturn(Optional.empty());
        when(marketDataService.search("NVDA")).thenReturn(List.of(
                new SymbolSearchResponse("NVDA", "NVIDIA Corporation", "NASDAQ", InstrumentType.STOCK, "USD")
        ));
        when(instrumentRepository.save(any(Instrument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instrument instrument = resolutionService.resolveUsdInstrument("nvda");

        assertEquals("NVDA", instrument.getTicker());
        assertEquals("NVIDIA Corporation", instrument.getName());
        assertEquals("NASDAQ", instrument.getExchange());
        assertEquals(InstrumentType.STOCK, instrument.getType());
        verify(instrumentRepository).save(instrument);
    }

    @Test
    void resolveUsdInstrument_rejectsExistingNonUsdInstrument() {
        Instrument instrument = new Instrument();
        instrument.setTicker("TSM");
        instrument.setCurrency("TWD");
        when(instrumentRepository.findByTickerIgnoreCase("TSM")).thenReturn(Optional.of(instrument));

        DomainValidationException exception = assertThrows(
                DomainValidationException.class,
                () -> resolutionService.resolveUsdInstrument("TSM")
        );

        assertEquals("only USD instruments supported in v1", exception.getMessage());
    }
}
