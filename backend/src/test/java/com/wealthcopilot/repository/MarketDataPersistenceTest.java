package com.wealthcopilot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wealthcopilot.entity.ApiKey;
import com.wealthcopilot.entity.ApiKeyScope;
import com.wealthcopilot.entity.Instrument;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.entity.PriceCache;
import com.wealthcopilot.entity.Transaction;
import com.wealthcopilot.entity.TransactionSide;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MarketDataPersistenceTest {

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private PriceCacheRepository priceCacheRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void priceCache_usesInstrumentAsPrimaryKeyAndPreservesQuoteFields() {
        Instrument instrument = saveInstrument("NVDA");
        LocalDateTime asOf = LocalDateTime.parse("2026-07-28T10:15:30.123");
        LocalDateTime fetchedAt = LocalDateTime.parse("2026-07-28T10:15:31.456");

        PriceCache priceCache = new PriceCache();
        priceCache.setInstrument(instrument);
        priceCache.setPrice(new BigDecimal("181.1234"));
        priceCache.setPreviousClose(null);
        priceCache.setAsOf(asOf);
        priceCache.setFetchedAt(fetchedAt);

        PriceCache saved = priceCacheRepository.saveAndFlush(priceCache);
        PriceCache found = priceCacheRepository.findByInstrumentTickerIgnoreCase("nvda").orElseThrow();

        assertEquals(instrument.getId(), saved.getInstrumentId());
        assertEquals(instrument.getId(), found.getInstrumentId());
        assertEquals(new BigDecimal("181.1234"), found.getPrice());
        assertNull(found.getPreviousClose());
        assertEquals(asOf, found.getAsOf());
        assertEquals(fetchedAt, found.getFetchedAt());
        assertEquals(1, priceCacheRepository.count());
    }

    @Test
    void apiKey_assignsDefaultsAndFindsOnlyActiveKeys() {
        String keyHash = "a".repeat(64);
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash(keyHash);
        apiKey.setLabel("Instructor demo key");
        apiKey.setScope(null);

        ApiKey saved = apiKeyRepository.saveAndFlush(apiKey);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(ApiKeyScope.READ_ONLY, saved.getScope());
        assertTrue(apiKeyRepository.existsByKeyHash(keyHash));
        assertTrue(apiKeyRepository.findByKeyHashAndRevokedAtIsNull(keyHash).isPresent());

        saved.setRevokedAt(LocalDateTime.parse("2026-07-28T11:00:00"));
        apiKeyRepository.saveAndFlush(saved);

        assertFalse(apiKeyRepository.findByKeyHashAndRevokedAtIsNull(keyHash).isPresent());
    }

    @Test
    void apiKey_rejectsDuplicateHashes() {
        String keyHash = "b".repeat(64);
        apiKeyRepository.saveAndFlush(apiKey(keyHash, "First key"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> apiKeyRepository.saveAndFlush(apiKey(keyHash, "Duplicate key"))
        );
    }

    @Test
    void currentlyHeldInstruments_excludesClosedPositions() {
        Instrument closed = saveInstrument("AAPL");
        Instrument held = saveInstrument("NVDA");
        transactionRepository.save(transaction(closed, TransactionSide.BUY, "2"));
        transactionRepository.save(transaction(closed, TransactionSide.SELL, "2"));
        transactionRepository.saveAndFlush(transaction(held, TransactionSide.BUY, "1.5"));

        var instruments = transactionRepository.findAllCurrentlyHeldInstruments();

        assertEquals(1, instruments.size());
        assertEquals("NVDA", instruments.get(0).getTicker());
    }

    private Instrument saveInstrument(String ticker) {
        Instrument instrument = new Instrument();
        instrument.setTicker(ticker);
        instrument.setExchange("NASDAQ");
        instrument.setName(ticker);
        instrument.setType(InstrumentType.STOCK);
        instrument.setCurrency("USD");
        return instrumentRepository.saveAndFlush(instrument);
    }

    private ApiKey apiKey(String keyHash, String label) {
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash(keyHash);
        apiKey.setLabel(label);
        return apiKey;
    }

    private Transaction transaction(Instrument instrument, TransactionSide side, String quantity) {
        Transaction transaction = new Transaction();
        transaction.setUserId(1L);
        transaction.setInstrument(instrument);
        transaction.setSide(side);
        transaction.setQuantity(new BigDecimal(quantity));
        transaction.setPrice(new BigDecimal("100.0000"));
        transaction.setTradeDate(LocalDate.of(2026, 7, 28));
        return transaction;
    }
}
