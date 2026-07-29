package com.wealthcopilot.controller;

import com.wealthcopilot.dto.request.TransactionUpsertRequest;
import com.wealthcopilot.dto.response.TransactionResponse;
import com.wealthcopilot.dto.response.TransactionPageResponse;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.service.TransactionService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public TransactionPageResponse list(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) TransactionSide side,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new DomainValidationException("page must be non-negative and size must be between 1 and 100");
        }

        List<TransactionResponse> filtered = transactionService.listTransactions(userId, ticker, from, to)
                .stream()
                .filter(transaction -> side == null || transaction.side() == side)
                .sorted(Comparator.comparing(TransactionResponse::tradeDate)
                        .thenComparing(TransactionResponse::id)
                        .reversed())
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        int start = Math.min((int) pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        PageImpl<TransactionResponse> result = new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
        return new TransactionPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        return transactionService.getTransaction(userId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(
            @RequestAttribute("userId") Long userId,
            @RequestBody TransactionUpsertRequest request
    ) {
        return transactionService.createTransaction(userId, request);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestBody TransactionUpsertRequest request
    ) {
        return transactionService.updateTransaction(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        transactionService.deleteTransaction(userId, id);
    }
}
