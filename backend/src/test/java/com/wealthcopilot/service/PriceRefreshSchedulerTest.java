package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.repository.TransactionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceRefreshSchedulerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PriceRefreshService refreshService;

    @Mock
    private MarketHours marketHours;

    private PriceRefreshQueue refreshQueue;
    private PriceRefreshScheduler scheduler;

    @BeforeEach
    void setUp() {
        refreshQueue = new PriceRefreshQueue();
        scheduler = new PriceRefreshScheduler(
                transactionRepository,
                refreshQueue,
                refreshService,
                marketHours
        );
    }

    @Test
    void enqueueHeldInstruments_addsCurrentHoldingsDuringMarketHours() {
        Instrument instrument = new Instrument();
        instrument.setId(1L);
        instrument.setTicker("NVDA");
        when(marketHours.isOpen()).thenReturn(true);
        when(transactionRepository.findAllCurrentlyHeldInstruments()).thenReturn(List.of(instrument));

        scheduler.enqueueHeldInstruments();

        assertEquals(1, refreshQueue.size());
    }

    @Test
    void enqueueHeldInstruments_doesNotQueryHoldingsOutsideMarketHours() {
        when(marketHours.isOpen()).thenReturn(false);

        scheduler.enqueueHeldInstruments();

        verify(transactionRepository, never()).findAllCurrentlyHeldInstruments();
    }

    @Test
    void refreshQueuedPrices_delegatesToRefreshService() {
        scheduler.refreshQueuedPrices();

        verify(refreshService).refreshNextBatch();
    }
}
