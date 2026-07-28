package com.wealthcopilot.repository;

import com.wealthcopilot.entity.Transaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findAllByUserIdOrderByTradeDateAscIdAsc(Long userId);

    List<Transaction> findAllByUserIdAndTradeDateBetweenOrderByTradeDateAscIdAsc(
            Long userId,
            LocalDate from,
            LocalDate to
    );

    List<Transaction> findAllByUserIdAndInstrumentTickerIgnoreCaseOrderByTradeDateAscIdAsc(
            Long userId,
            String ticker
    );
}
