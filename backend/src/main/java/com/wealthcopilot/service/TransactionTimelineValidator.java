package com.wealthcopilot.service;

import com.wealthcopilot.entity.Transaction;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.exception.DomainValidationException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TransactionTimelineValidator {

    public void validate(List<Transaction> timeline) {
        Map<String, BigDecimal> positions = new HashMap<>();

        for (Transaction tx : timeline) {
            String ticker = tx.getInstrument().getTicker().toUpperCase();
            BigDecimal current = positions.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal next = tx.getSide() == TransactionSide.BUY
                    ? current.add(tx.getQuantity())
                    : current.subtract(tx.getQuantity());

            if (next.signum() < 0) {
                throw new DomainValidationException(
                        "sell of " + tx.getQuantity().stripTrailingZeros().toPlainString()
                                + " " + ticker
                                + " on " + tx.getTradeDate()
                                + " would exceed position ("
                                + current.stripTrailingZeros().toPlainString() + ") after this change"
                );
            }

            positions.put(ticker, next);
        }
    }
}
