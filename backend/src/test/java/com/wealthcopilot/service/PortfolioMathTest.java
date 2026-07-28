package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.entity.Transaction;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.entity.TransactionSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortfolioMathTest {

    @Test
    void replay_calculatesAverageCostAndRealizedPnl() {
        Transaction buy1 = tx(1L, "NVDA", TransactionSide.BUY, "10", "100", "0", LocalDate.parse("2026-01-01"));
        Transaction buy2 = tx(2L, "NVDA", TransactionSide.BUY, "10", "120", "2", LocalDate.parse("2026-01-02"));
        Transaction sell = tx(3L, "NVDA", TransactionSide.SELL, "8", "130", "1", LocalDate.parse("2026-01-03"));

        PortfolioMath.Result result = PortfolioMath.replay(List.of(buy1, buy2, sell));

        PortfolioMath.PositionState nvda = result.positions().get("NVDA");
        assertEquals(new BigDecimal("12.000000"), nvda.getTotalQuantity());
        assertTrue(nvda.getTotalCost().compareTo(new BigDecimal("1321.2")) == 0);
        assertEquals(new BigDecimal("110.1000"), nvda.avgCost());
        assertEquals(new BigDecimal("158.2000"), result.realizedPnl());
    }

    @Test
    void replay_handlesFullExitWithoutResidualCost() {
        Transaction buy = tx(1L, "SPY", TransactionSide.BUY, "5", "400", "0", LocalDate.parse("2026-02-01"));
        Transaction sell = tx(2L, "SPY", TransactionSide.SELL, "5", "410", "0", LocalDate.parse("2026-02-02"));

        PortfolioMath.Result result = PortfolioMath.replay(List.of(buy, sell));

        PortfolioMath.PositionState spy = result.positions().get("SPY");
        assertEquals(new BigDecimal("0.000000"), spy.getTotalQuantity());
        assertEquals(new BigDecimal("0"), spy.getTotalCost().stripTrailingZeros());
        assertEquals(new BigDecimal("50.0000"), result.realizedPnl());
    }

    private static Transaction tx(
            Long id,
            String ticker,
            TransactionSide side,
            String quantity,
            String price,
            String fees,
            LocalDate tradeDate
    ) {
        Instrument instrument = new Instrument();
        instrument.setTicker(ticker);
        instrument.setName(ticker);
        instrument.setType(InstrumentType.STOCK);
        instrument.setCurrency("USD");

        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setUserId(1L);
        tx.setInstrument(instrument);
        tx.setSide(side);
        tx.setQuantity(new BigDecimal(quantity));
        tx.setPrice(new BigDecimal(price));
        tx.setFees(new BigDecimal(fees));
        tx.setTradeDate(tradeDate);
        tx.setSource(TransactionSource.MANUAL);
        return tx;
    }
}
