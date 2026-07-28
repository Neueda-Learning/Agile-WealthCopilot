package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.dto.request.TransactionUpsertRequest;
import com.wealthcopilot.dto.response.TransactionResponse;
import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.entity.Transaction;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.entity.TransactionSource;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.repository.InstrumentRepository;
import com.wealthcopilot.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private TransactionTimelineValidator timelineValidator;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createTransaction_savesWhenValidUsdAndBalancedTimeline() {
        Long userId = 7L;
        Instrument instrument = instrument("NVDA", "USD");
        Transaction existing = tx(1L, userId, instrument, TransactionSide.BUY, "10", "100", "0", "2026-01-01");

        when(instrumentRepository.findByTickerIgnoreCase("NVDA")).thenReturn(Optional.of(instrument));
        when(transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)).thenReturn(new ArrayList<>(List.of(existing)));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        TransactionUpsertRequest request = new TransactionUpsertRequest(
                "NVDA",
                TransactionSide.BUY,
                new BigDecimal("5"),
                new BigDecimal("110"),
                BigDecimal.ZERO,
                LocalDate.parse("2026-01-02"),
                "test",
                TransactionSource.MANUAL
        );

        TransactionResponse response = transactionService.createTransaction(userId, request);

        assertEquals(2L, response.id());
        assertEquals("NVDA", response.ticker());
        assertEquals(new BigDecimal("5"), response.quantity());
        verify(timelineValidator).validate(any(List.class));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals(userId, txCaptor.getValue().getUserId());
        assertEquals(TransactionSide.BUY, txCaptor.getValue().getSide());
    }

    @Test
    void createTransaction_throwsWhenTickerUnknown() {
        when(instrumentRepository.findByTickerIgnoreCase("NOPE")).thenReturn(Optional.empty());

        TransactionUpsertRequest request = new TransactionUpsertRequest(
                "NOPE",
                TransactionSide.BUY,
                new BigDecimal("1"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                LocalDate.parse("2026-01-01"),
                null,
                TransactionSource.MANUAL
        );

        DomainValidationException ex = assertThrows(
                DomainValidationException.class,
                () -> transactionService.createTransaction(1L, request)
        );

        assertTrue(ex.getMessage().contains("unknown ticker"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_throwsWhenInstrumentCurrencyNotUsd() {
        when(instrumentRepository.findByTickerIgnoreCase("TSM")).thenReturn(Optional.of(instrument("TSM", "TWD")));

        TransactionUpsertRequest request = new TransactionUpsertRequest(
                "TSM",
                TransactionSide.BUY,
                new BigDecimal("1"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                LocalDate.parse("2026-01-01"),
                null,
                TransactionSource.MANUAL
        );

        DomainValidationException ex = assertThrows(
                DomainValidationException.class,
                () -> transactionService.createTransaction(1L, request)
        );

        assertEquals("only USD instruments supported in v1", ex.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_throwsWhenTargetMissing() {
        when(instrumentRepository.findByTickerIgnoreCase("NVDA")).thenReturn(Optional.of(instrument("NVDA", "USD")));
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        TransactionUpsertRequest request = new TransactionUpsertRequest(
                "NVDA",
                TransactionSide.BUY,
                new BigDecimal("1"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                LocalDate.parse("2026-01-01"),
                null,
                TransactionSource.MANUAL
        );

        assertThrows(ResourceNotFoundException.class, () -> transactionService.updateTransaction(1L, 99L, request));
    }

    @Test
    void deleteTransaction_revalidatesTimelineBeforeDelete() {
        Long userId = 1L;
        Instrument instrument = instrument("AAPL", "USD");
        Transaction toDelete = tx(1L, userId, instrument, TransactionSide.BUY, "3", "100", "0", "2026-01-01");
        Transaction keep = tx(2L, userId, instrument, TransactionSide.BUY, "2", "105", "0", "2026-01-02");

        when(transactionRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(toDelete));
        when(transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)).thenReturn(new ArrayList<>(List.of(toDelete, keep)));

        transactionService.deleteTransaction(userId, 1L);

        ArgumentCaptor<List<Transaction>> timelineCaptor = ArgumentCaptor.forClass(List.class);
        verify(timelineValidator).validate(timelineCaptor.capture());
        assertEquals(1, timelineCaptor.getValue().size());
        assertEquals(2L, timelineCaptor.getValue().get(0).getId());
        verify(transactionRepository).delete(eq(toDelete));
    }

    private static Instrument instrument(String ticker, String currency) {
        Instrument instrument = new Instrument();
        instrument.setTicker(ticker);
        instrument.setName(ticker);
        instrument.setType(InstrumentType.STOCK);
        instrument.setCurrency(currency);
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
