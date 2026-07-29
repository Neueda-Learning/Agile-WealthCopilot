package com.wealthcopilot.dto.response;

import com.wealthcopilot.entity.TransactionSide;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A transaction the user is asked to confirm. Never saved directly.
 *
 * @param transactionId null for a new entry; set when the draft edits an
 *                      existing one, so the UI knows to update rather than add.
 */
public record TransactionDraftResponse(
        Long transactionId,
        String ticker,
        TransactionSide side,
        BigDecimal quantity,
        BigDecimal price,
        LocalDate tradeDate
) {

    public static TransactionDraftResponse newEntry(
            String ticker,
            TransactionSide side,
            BigDecimal quantity,
            BigDecimal price,
            LocalDate tradeDate
    ) {
        return new TransactionDraftResponse(null, ticker, side, quantity, price, tradeDate);
    }
}
