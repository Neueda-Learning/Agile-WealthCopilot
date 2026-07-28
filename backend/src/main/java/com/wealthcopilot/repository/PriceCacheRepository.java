package com.wealthcopilot.repository;

import com.wealthcopilot.entity.PriceCache;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PriceCacheRepository extends JpaRepository<PriceCache, Long> {

    Optional<PriceCache> findByInstrumentId(Long instrumentId);

    Optional<PriceCache> findByInstrumentTickerIgnoreCase(String ticker);

    @Query("select max(price.fetchedAt) from PriceCache price")
    Optional<LocalDateTime> findLatestFetchedAt();
}
