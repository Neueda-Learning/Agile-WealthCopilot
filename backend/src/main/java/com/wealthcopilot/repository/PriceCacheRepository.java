package com.wealthcopilot.repository;

import com.wealthcopilot.entity.PriceCache;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceCacheRepository extends JpaRepository<PriceCache, Long> {

    Optional<PriceCache> findByInstrumentId(Long instrumentId);

    Optional<PriceCache> findByInstrumentTickerIgnoreCase(String ticker);
}
