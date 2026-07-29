package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wealthcopilot.entity.Instrument;
import org.junit.jupiter.api.Test;

class PriceRefreshQueueTest {

    @Test
    void queue_deduplicatesQueuedAndInFlightInstruments() {
        PriceRefreshQueue queue = new PriceRefreshQueue();
        Instrument instrument = instrument(1L, "AAPL");
        queue.enqueue(instrument);
        queue.enqueue(instrument);

        var firstBatch = queue.drain(8);
        queue.enqueue(instrument);

        assertEquals(1, firstBatch.size());
        assertEquals(0, queue.size());
        assertTrue(queue.drain(8).isEmpty());

        queue.complete(instrument);
        queue.enqueue(instrument);

        assertEquals(1, queue.size());
    }

    @Test
    void retry_releasesInFlightInstrumentAndQueuesItAgain() {
        PriceRefreshQueue queue = new PriceRefreshQueue();
        Instrument instrument = instrument(1L, "AAPL");
        queue.enqueue(instrument);
        queue.drain(8);

        queue.retry(instrument);

        assertEquals(1, queue.size());
    }

    @Test
    void removeQueued_doesNotReleaseAnInFlightSchedulerRequest() {
        PriceRefreshQueue queue = new PriceRefreshQueue();
        Instrument instrument = instrument(1L, "AAPL");
        queue.enqueue(instrument);
        queue.drain(8);

        queue.removeQueued(instrument);
        queue.enqueue(instrument);

        assertEquals(0, queue.size());
        assertTrue(queue.drain(8).isEmpty());
    }

    private Instrument instrument(Long id, String ticker) {
        Instrument instrument = new Instrument();
        instrument.setId(id);
        instrument.setTicker(ticker);
        return instrument;
    }
}
