package com.wealthcopilot.service;

import com.wealthcopilot.entity.Instrument;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PriceRefreshQueue {

    private final ConcurrentHashMap<Long, Instrument> queued = new ConcurrentHashMap<>();
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public synchronized void enqueue(Instrument instrument) {
        if (instrument != null
                && instrument.getId() != null
                && !inFlight.contains(instrument.getId())) {
            queued.putIfAbsent(instrument.getId(), instrument);
        }
    }

    public synchronized List<Instrument> drain(int limit) {
        int safeLimit = Math.max(0, limit);
        List<Instrument> drained = new ArrayList<>(safeLimit);
        for (Instrument instrument : queued.values()) {
            if (drained.size() >= safeLimit) {
                break;
            }
            Long instrumentId = instrument.getId();
            if (inFlight.add(instrumentId)) {
                if (queued.remove(instrumentId, instrument)) {
                    drained.add(instrument);
                } else {
                    inFlight.remove(instrumentId);
                }
            }
        }
        return List.copyOf(drained);
    }

    public synchronized void complete(Instrument instrument) {
        if (instrument != null && instrument.getId() != null) {
            inFlight.remove(instrument.getId());
        }
    }

    public synchronized void retry(Instrument instrument) {
        complete(instrument);
        enqueue(instrument);
    }

    public synchronized int size() {
        return queued.size();
    }
}
