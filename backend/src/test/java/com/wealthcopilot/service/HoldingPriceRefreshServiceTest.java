package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.dto.response.PriceRefreshResponse;
import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HoldingPriceRefreshServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PriceRefreshService priceRefreshService;

    @InjectMocks
    private HoldingPriceRefreshService service;

    @Test
    void refreshHeldPrices_refreshesOnlyTheUsersOpenPositions() {
        Instrument apple = instrument(1L, "AAPL");
        Instrument nvidia = instrument(2L, "NVDA");
        LocalDateTime completedAt = LocalDateTime.parse("2026-07-29T12:00:00");
        when(transactionRepository.findAllCurrentlyHeldInstrumentsByUserId(7L))
                .thenReturn(List.of(apple, nvidia));
        when(priceRefreshService.refreshInstrumentsNow(List.of(apple, nvidia)))
                .thenReturn(new PriceRefreshService.RefreshResult(
                        2, 1, List.of("NVDA"), completedAt));

        PriceRefreshResponse response = service.refreshHeldPrices(7L);

        assertEquals(2, response.requested());
        assertEquals(1, response.refreshed());
        assertEquals(List.of("NVDA"), response.failedTickers());
        assertEquals(completedAt, response.completedAt());
        verify(transactionRepository).findAllCurrentlyHeldInstrumentsByUserId(7L);
    }

    private Instrument instrument(Long id, String ticker) {
        Instrument instrument = new Instrument();
        instrument.setId(id);
        instrument.setTicker(ticker);
        return instrument;
    }
}
