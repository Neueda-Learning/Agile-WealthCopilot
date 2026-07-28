package com.wealthcopilot.service;

import com.wealthcopilot.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PriceRefreshScheduler {

    private final TransactionRepository transactionRepository;
    private final PriceRefreshQueue refreshQueue;
    private final PriceRefreshService refreshService;
    private final MarketHours marketHours;

    public PriceRefreshScheduler(
            TransactionRepository transactionRepository,
            PriceRefreshQueue refreshQueue,
            PriceRefreshService refreshService,
            MarketHours marketHours
    ) {
        this.transactionRepository = transactionRepository;
        this.refreshQueue = refreshQueue;
        this.refreshService = refreshService;
        this.marketHours = marketHours;
    }

    @Scheduled(cron = "0 */15 * * * *", zone = "${market-data.market-zone:America/New_York}")
    public void enqueueHeldInstruments() {
        if (!marketHours.isOpen()) {
            return;
        }
        transactionRepository.findAllCurrentlyHeldInstruments().forEach(refreshQueue::enqueue);
    }

    @Scheduled(cron = "0 * * * * *")
    public void refreshQueuedPrices() {
        refreshService.refreshNextBatch();
    }
}
