package com.wealthcopilot.controller;

import com.wealthcopilot.dto.request.TransactionUpsertRequest;
import com.wealthcopilot.dto.response.TransactionResponse;
import com.wealthcopilot.service.TransactionService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<TransactionResponse> list(
            @RequestAttribute("userId") Long userId,
            String ticker,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return transactionService.listTransactions(userId, ticker, from, to);
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
