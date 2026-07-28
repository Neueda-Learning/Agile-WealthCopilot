package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wealthcopilot.marketdata.MarketDataProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MarketHoursTest {

    private final MarketDataProperties properties = new MarketDataProperties();

    @Test
    void isOpen_acceptsWeekdayAtNewYorkOpen() {
        assertTrue(marketHoursAt("2026-07-28T13:30:00Z").isOpen());
    }

    @Test
    void isOpen_rejectsWeekdayAtNewYorkClose() {
        assertFalse(marketHoursAt("2026-07-28T20:00:00Z").isOpen());
    }

    @Test
    void isOpen_rejectsWeekendDuringTradingHours() {
        assertFalse(marketHoursAt("2026-08-01T14:00:00Z").isOpen());
    }

    private MarketHours marketHoursAt(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new MarketHours(properties, clock);
    }
}
