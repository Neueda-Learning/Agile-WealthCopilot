package com.wealthcopilot.repository;

import com.wealthcopilot.entity.Instrument;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findByTickerIgnoreCase(String ticker);
}
