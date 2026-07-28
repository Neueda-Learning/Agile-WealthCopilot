package com.wealthcopilot.service;

import com.wealthcopilot.dto.response.HoldingResponse;
import com.wealthcopilot.dto.response.PerformanceSummaryResponse;
import com.wealthcopilot.dto.response.PortfolioSummaryResponse;
import com.wealthcopilot.dto.response.TransactionResponse;
import java.time.LocalDate;
import java.util.List;

public interface EzioAgentToolService {

    PortfolioSummaryResponse getPortfolioSummary(Long userId);

    List<HoldingResponse> getHoldings(Long userId);

    List<TransactionResponse> getTransactions(Long userId, String ticker, LocalDate from, LocalDate to);

    PerformanceSummaryResponse getInvestedAmount(Long userId, LocalDate from, LocalDate to);
}
