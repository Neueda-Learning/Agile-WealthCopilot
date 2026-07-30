package com.wealthcopilot.repository;

import com.wealthcopilot.entity.PriceCache;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PriceCacheRepository extends JpaRepository<PriceCache, Long> {

    Optional<PriceCache> findByInstrumentId(Long instrumentId);

    List<PriceCache> findAllByInstrumentIdIn(Collection<Long> instrumentIds);

    Optional<PriceCache> findByInstrumentTickerIgnoreCase(String ticker);

    @Query("select max(price.fetchedAt) from PriceCache price")
    Optional<LocalDateTime> findLatestFetchedAt();
}
