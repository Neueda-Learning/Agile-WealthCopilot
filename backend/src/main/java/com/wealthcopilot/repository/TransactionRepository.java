package com.wealthcopilot.repository;

import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.Transaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query("""
            select transaction.instrument
            from Transaction transaction
            group by transaction.instrument
            having sum(
                case
                    when transaction.side = com.wealthcopilot.entity.TransactionSide.BUY
                    then transaction.quantity
                    else -transaction.quantity
                end
            ) > 0
            """)
    List<Instrument> findAllCurrentlyHeldInstruments();
}
