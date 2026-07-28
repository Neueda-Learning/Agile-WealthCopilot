package com.wealthcopilot.service;

import com.wealthcopilot.dto.request.TransactionUpsertRequest;
import com.wealthcopilot.dto.response.TransactionResponse;
import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.Transaction;
import com.wealthcopilot.entity.TransactionSource;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.repository.InstrumentRepository;
import com.wealthcopilot.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final InstrumentRepository instrumentRepository;
    private final TransactionTimelineValidator timelineValidator;

    public TransactionService(
            TransactionRepository transactionRepository,
            InstrumentRepository instrumentRepository,
            TransactionTimelineValidator timelineValidator
    ) {
        this.transactionRepository = transactionRepository;
        this.instrumentRepository = instrumentRepository;
        this.timelineValidator = timelineValidator;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(Long userId) {
        return transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(Long userId, String ticker, LocalDate from, LocalDate to) {
        if (ticker != null && !ticker.isBlank()) {
            return transactionRepository
                    .findAllByUserIdAndInstrumentTickerIgnoreCaseOrderByTradeDateAscIdAsc(userId, ticker)
                    .stream()
                    .filter(tx -> withinRange(tx.getTradeDate(), from, to))
                    .map(this::toResponse)
                    .toList();
        }

        if (from != null && to != null) {
            return transactionRepository.findAllByUserIdAndTradeDateBetweenOrderByTradeDateAscIdAsc(userId, from, to)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return listTransactions(userId);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long userId, Long transactionId) {
        Transaction tx = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("transaction not found"));
        return toResponse(tx);
    }

    @Transactional
    @CacheEvict(cacheNames = {"portfolioSummary", "portfolioHoldings"}, key = "#userId")
    public TransactionResponse createTransaction(Long userId, TransactionUpsertRequest request) {
        validateRequest(request);
        Instrument instrument = resolveUsdInstrument(request.ticker());

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setInstrument(instrument);
        applyRequest(tx, request);

        List<Transaction> candidateTimeline = new ArrayList<>(
                transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)
        );
        candidateTimeline.add(tx);
        candidateTimeline.sort((a, b) -> {
            int byDate = a.getTradeDate().compareTo(b.getTradeDate());
            if (byDate != 0) {
                return byDate;
            }
            long left = a.getId() == null ? Long.MAX_VALUE : a.getId();
            long right = b.getId() == null ? Long.MAX_VALUE : b.getId();
            return Long.compare(left, right);
        });
        timelineValidator.validate(candidateTimeline);

        return toResponse(transactionRepository.save(tx));
    }

    @Transactional
    @CacheEvict(cacheNames = {"portfolioSummary", "portfolioHoldings"}, key = "#userId")
    public TransactionResponse updateTransaction(Long userId, Long transactionId, TransactionUpsertRequest request) {
        validateRequest(request);
        Instrument instrument = resolveUsdInstrument(request.ticker());

        Transaction existing = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("transaction not found"));

        Transaction copy = cloneForValidation(existing);
        copy.setInstrument(instrument);
        applyRequest(copy, request);

        List<Transaction> candidateTimeline = new ArrayList<>(
                transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)
        );
        for (int i = 0; i < candidateTimeline.size(); i++) {
            if (candidateTimeline.get(i).getId().equals(transactionId)) {
                candidateTimeline.set(i, copy);
                break;
            }
        }
        candidateTimeline.sort((a, b) -> {
            int byDate = a.getTradeDate().compareTo(b.getTradeDate());
            if (byDate != 0) {
                return byDate;
            }
            return a.getId().compareTo(b.getId());
        });
        timelineValidator.validate(candidateTimeline);

        existing.setInstrument(instrument);
        applyRequest(existing, request);
        return toResponse(transactionRepository.save(existing));
    }

    @Transactional
    @CacheEvict(cacheNames = {"portfolioSummary", "portfolioHoldings"}, key = "#userId")
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction existing = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("transaction not found"));

        List<Transaction> candidateTimeline = new ArrayList<>(
                transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId)
        );
        candidateTimeline.removeIf(tx -> tx.getId().equals(transactionId));
        timelineValidator.validate(candidateTimeline);

        transactionRepository.delete(existing);
    }

    private boolean withinRange(LocalDate value, LocalDate from, LocalDate to) {
        if (from != null && value.isBefore(from)) {
            return false;
        }
        if (to != null && value.isAfter(to)) {
            return false;
        }
        return true;
    }

    private Transaction cloneForValidation(Transaction existing) {
        Transaction clone = new Transaction();
        clone.setId(existing.getId());
        clone.setUserId(existing.getUserId());
        clone.setInstrument(existing.getInstrument());
        clone.setSide(existing.getSide());
        clone.setQuantity(existing.getQuantity());
        clone.setPrice(existing.getPrice());
        clone.setFees(existing.getFees());
        clone.setTradeDate(existing.getTradeDate());
        clone.setNote(existing.getNote());
        clone.setSource(existing.getSource());
        return clone;
    }

    private void applyRequest(Transaction tx, TransactionUpsertRequest request) {
        tx.setSide(request.side());
        tx.setQuantity(request.quantity());
        tx.setPrice(request.price());
        tx.setFees(request.fees() == null ? BigDecimal.ZERO : request.fees());
        tx.setTradeDate(request.tradeDate());
        tx.setNote(request.note());
        tx.setSource(request.source() == null ? TransactionSource.MANUAL : request.source());
    }

    private void validateRequest(TransactionUpsertRequest request) {
        if (request == null) {
            throw new DomainValidationException("request is required");
        }
        if (request.ticker() == null || request.ticker().isBlank()) {
            throw new DomainValidationException("ticker is required");
        }
        if (request.side() == null) {
            throw new DomainValidationException("side is required");
        }
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            throw new DomainValidationException("quantity must be greater than 0");
        }
        if (request.price() == null || request.price().signum() < 0) {
            throw new DomainValidationException("price must be greater than or equal to 0");
        }
        if (request.fees() != null && request.fees().signum() < 0) {
            throw new DomainValidationException("fees must be greater than or equal to 0");
        }
        if (request.tradeDate() == null) {
            throw new DomainValidationException("tradeDate is required");
        }
    }

    private Instrument resolveUsdInstrument(String ticker) {
        Instrument instrument = instrumentRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new DomainValidationException("unknown ticker: " + ticker));
        if (!"USD".equalsIgnoreCase(instrument.getCurrency())) {
            throw new DomainValidationException("only USD instruments supported in v1");
        }
        return instrument;
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getInstrument().getTicker(),
                tx.getInstrument().getName(),
                tx.getSide(),
                tx.getQuantity(),
                tx.getPrice(),
                tx.getFees(),
                tx.getTradeDate(),
                tx.getNote(),
                tx.getSource(),
                tx.getCreatedAt()
        );
    }
}
