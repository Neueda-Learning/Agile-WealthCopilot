package com.wealthcopilot.service;

import com.wealthcopilot.dto.response.PriceRefreshResponse;
import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.repository.TransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HoldingPriceRefreshService {

    private final TransactionRepository transactionRepository;
    private final PriceRefreshService priceRefreshService;

    public HoldingPriceRefreshService(
            TransactionRepository transactionRepository,
            PriceRefreshService priceRefreshService
    ) {
        this.transactionRepository = transactionRepository;
        this.priceRefreshService = priceRefreshService;
    }

    public PriceRefreshResponse refreshHeldPrices(Long userId) {
        List<Instrument> instruments =
                transactionRepository.findAllCurrentlyHeldInstrumentsByUserId(userId);
        PriceRefreshService.RefreshResult result =
                priceRefreshService.refreshInstrumentsNow(instruments);
        return new PriceRefreshResponse(
                result.requested(),
                result.refreshed(),
                result.failedTickers(),
                result.completedAt());
    }
}
