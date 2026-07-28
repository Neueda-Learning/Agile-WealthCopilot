package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.entity.Transaction;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.entity.TransactionSource;
import com.wealthcopilot.exception.DomainValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionTimelineValidatorTest {

    private final TransactionTimelineValidator validator = new TransactionTimelineValidator();

    @Test
    void validate_allowsBalancedTimeline() {
        List<Transaction> timeline = List.of(
                tx(1L, "AAPL", TransactionSide.BUY, "10", "2026-01-01"),
                tx(2L, "AAPL", TransactionSide.SELL, "6", "2026-01-02"),
                tx(3L, "AAPL", TransactionSide.SELL, "4", "2026-01-03")
        );

        assertDoesNotThrow(() -> validator.validate(timeline));
    }

    @Test
    void validate_rejectsSellThatExceedsHoldings() {
        List<Transaction> timeline = List.of(
                tx(1L, "NVDA", TransactionSide.BUY, "5", "2026-01-01"),
                tx(2L, "NVDA", TransactionSide.SELL, "7", "2026-01-02")
        );

        assertThrows(DomainValidationException.class, () -> validator.validate(timeline));
    }

    @Test
    void validate_rejectsBrokenTimelineAfterEarlierBuyRemoved() {
        List<Transaction> timeline = List.of(
                tx(2L, "MSFT", TransactionSide.BUY, "2", "2026-01-02"),
                tx(3L, "MSFT", TransactionSide.SELL, "3", "2026-01-03")
        );

        assertThrows(DomainValidationException.class, () -> validator.validate(timeline));
    }

    private static Transaction tx(Long id, String ticker, TransactionSide side, String qty, String date) {
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
        tx.setQuantity(new BigDecimal(qty));
        tx.setPrice(new BigDecimal("100"));
        tx.setFees(BigDecimal.ZERO);
        tx.setTradeDate(LocalDate.parse(date));
        tx.setSource(TransactionSource.MANUAL);
        return tx;
    }
}
