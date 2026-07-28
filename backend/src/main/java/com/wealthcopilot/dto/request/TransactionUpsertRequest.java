package com.wealthcopilot.dto.request;

import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.entity.TransactionSource;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionUpsertRequest(
        String ticker,
        TransactionSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fees,
        LocalDate tradeDate,
        String note,
        TransactionSource source
) {
}
