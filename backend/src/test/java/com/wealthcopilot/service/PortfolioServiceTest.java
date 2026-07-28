package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.dto.response.HoldingResponse;
import com.wealthcopilot.dto.response.PerformanceSummaryResponse;
import com.wealthcopilot.dto.response.PortfolioSummaryResponse;
import com.wealthcopilot.dto.response.TransactionResponse;
import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.entity.Transaction;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.entity.TransactionSource;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private PriceQuoteProvider priceQuoteProvider;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void getInvestedAmount_throwsWhenFromIsAfterTo() {
        assertThrows(
                DomainValidationException.class,
                () -> portfolioService.getInvestedAmount(1L, LocalDate.parse("2026-02-01"), LocalDate.parse("2026-01-01"))
        );
    }

    @Test
    void getInvestedAmount_calculatesCashFlowAndRealizedPnl() {
        Long userId = 1L;
        Instrument nvda = instrument("NVDA");
        List<Transaction> timeline = List.of(
                tx(1L, userId, nvda, TransactionSide.BUY, "10", "100", "2", "2026-01-01"),
                tx(2L, userId, nvda, TransactionSide.SELL, "4", "120", "1", "2026-01-02")
        );

        when(transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)).thenReturn(timeline);

        PerformanceSummaryResponse response = portfolioService.getInvestedAmount(
                userId,
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31")
        );

        assertEquals(new BigDecimal("1002.0000"), response.investedAmount());
        assertEquals(new BigDecimal("479.0000"), response.proceedsAmount());
        assertEquals(new BigDecimal("523.0000"), response.netInvested());
        assertEquals(new BigDecimal("78.2000"), response.realizedPnl());
        assertEquals(1, response.buyCount());
        assertEquals(1, response.sellCount());
        assertEquals("USD", response.currency());
    }

    @Test
    void getPortfolioSummary_computesStaleAndDayChangeFromQuotes() {
        Long userId = 1L;
        Instrument aapl = instrument("AAPL");
        List<Transaction> timeline = List.of(
                tx(1L, userId, aapl, TransactionSide.BUY, "2", "100", "0", "2026-01-01")
        );

        when(transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)).thenReturn(timeline);
        when(priceQuoteProvider.getLatestQuote("AAPL")).thenReturn(Optional.of(
                new PriceQuoteProvider.QuoteSnapshot(
                        new BigDecimal("110"),
                        new BigDecimal("100"),
                        LocalDateTime.parse("2026-01-03T10:00:00"),
                        true
                )
        ));

        PortfolioSummaryResponse summary = portfolioService.getPortfolioSummary(userId);

        assertEquals(new BigDecimal("220.0000"), summary.totalValue());
        assertEquals(new BigDecimal("200.0000"), summary.totalCostBasis());
        assertEquals(new BigDecimal("20.0000"), summary.unrealizedPnl());
        assertEquals(new BigDecimal("10.00"), summary.unrealizedPnlPct());
        assertEquals(new BigDecimal("20.0000"), summary.dayChange());
        assertEquals(new BigDecimal("10.00"), summary.dayChangePct());
        assertEquals(LocalDateTime.parse("2026-01-03T10:00:00"), summary.pricesAsOf());
        assertEquals(true, summary.stale());
    }

    @Test
    void getHoldings_calculatesWeightsAndSortsByTicker() {
        Long userId = 1L;
        Instrument aapl = instrument("AAPL");
        Instrument msft = instrument("MSFT");
        List<Transaction> timeline = List.of(
                tx(1L, userId, msft, TransactionSide.BUY, "1", "100", "0", "2026-01-01"),
                tx(2L, userId, aapl, TransactionSide.BUY, "1", "100", "0", "2026-01-01")
        );

        when(transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)).thenReturn(timeline);
        when(priceQuoteProvider.getLatestQuote("AAPL")).thenReturn(Optional.of(
                new PriceQuoteProvider.QuoteSnapshot(
                        new BigDecimal("120"),
                        new BigDecimal("100"),
                        LocalDateTime.parse("2026-01-03T10:00:00"),
                        false
                )
        ));
        when(priceQuoteProvider.getLatestQuote("MSFT")).thenReturn(Optional.of(
                new PriceQuoteProvider.QuoteSnapshot(
                        new BigDecimal("80"),
                        new BigDecimal("100"),
                        LocalDateTime.parse("2026-01-03T10:00:00"),
                        false
                )
        ));

        List<HoldingResponse> holdings = portfolioService.getHoldings(userId);

        assertEquals(2, holdings.size());
        assertEquals("AAPL", holdings.get(0).ticker());
        assertEquals("MSFT", holdings.get(1).ticker());

        assertEquals(new BigDecimal("60.00"), holdings.get(0).weightPct());
        assertEquals(new BigDecimal("40.00"), holdings.get(1).weightPct());
        assertEquals(new BigDecimal("20.00"), holdings.get(0).dayChangePct());
        assertEquals(new BigDecimal("-20.00"), holdings.get(1).dayChangePct());
    }

    @Test
    void getPortfolioSummary_returnsNullDayChangePctWhenNoPreviousClose() {
        Long userId = 1L;
        Instrument aapl = instrument("AAPL");
        List<Transaction> timeline = List.of(
                tx(1L, userId, aapl, TransactionSide.BUY, "2", "100", "0", "2026-01-01")
        );

        when(transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)).thenReturn(timeline);
        when(priceQuoteProvider.getLatestQuote("AAPL")).thenReturn(Optional.of(
                new PriceQuoteProvider.QuoteSnapshot(
                        new BigDecimal("110"),
                        null,
                        LocalDateTime.parse("2026-01-03T10:00:00"),
                        false
                )
        ));

        PortfolioSummaryResponse summary = portfolioService.getPortfolioSummary(userId);
        assertNull(summary.dayChangePct());
    }

    @Test
    void getTransactions_delegatesToTransactionService() {
        Long userId = 9L;
        List<TransactionResponse> expected = List.of(
                new TransactionResponse(
                        1L,
                        "NVDA",
                        "NVIDIA",
                        TransactionSide.BUY,
                        new BigDecimal("1"),
                        new BigDecimal("100"),
                        BigDecimal.ZERO,
                        LocalDate.parse("2026-01-01"),
                        null,
                        TransactionSource.MANUAL,
                        null
                )
        );

        when(transactionService.listTransactions(userId, "NVDA", null, null)).thenReturn(expected);

        List<TransactionResponse> actual = portfolioService.getTransactions(userId, "NVDA", null, null);

        assertEquals(expected, actual);
        verify(transactionService).listTransactions(userId, "NVDA", null, null);
    }

    private static Instrument instrument(String ticker) {
        Instrument instrument = new Instrument();
        instrument.setTicker(ticker);
        instrument.setName(ticker);
        instrument.setType(InstrumentType.STOCK);
        instrument.setCurrency("USD");
        return instrument;
    }

    private static Transaction tx(
            Long id,
            Long userId,
            Instrument instrument,
            TransactionSide side,
            String quantity,
            String price,
            String fees,
            String date
    ) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setUserId(userId);
        tx.setInstrument(instrument);
        tx.setSide(side);
        tx.setQuantity(new BigDecimal(quantity));
        tx.setPrice(new BigDecimal(price));
        tx.setFees(new BigDecimal(fees));
        tx.setTradeDate(LocalDate.parse(date));
        tx.setSource(TransactionSource.MANUAL);
        return tx;
    }
}
