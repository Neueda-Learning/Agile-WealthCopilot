package com.wealthcopilot.dto.response;

import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.entity.TransactionSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String ticker,
        String instrumentName,
        TransactionSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fees,
        LocalDate tradeDate,
        String note,
        TransactionSource source,
        LocalDateTime createdAt
) {
}
