package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wealthcopilot.marketdata.MarketDataProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceRefreshBudgetTest {

    private MutableClock clock;
    private PriceRefreshBudget budget;

    @BeforeEach
    void setUp() {
        MarketDataProperties properties = new MarketDataProperties();
        properties.setCreditsPerMinute(8);
        clock = new MutableClock(Instant.parse("2026-07-29T16:00:00Z"));
        budget = new PriceRefreshBudget(properties, clock);
    }

    @Test
    void tryConsume_grantsOnlyWhatTheMinuteAllowanceStillHas() {
        assertEquals(5, budget.tryConsume(5));
        assertEquals(3, budget.tryConsume(6));
        assertEquals(0, budget.tryConsume(1));
    }

    @Test
    void tryConsume_startsOverOnceTheMinuteElapses() {
        assertEquals(8, budget.tryConsume(20));
        assertEquals(0, budget.tryConsume(1));

        clock.advance(Duration.ofSeconds(61));

        assertEquals(8, budget.tryConsume(20));
    }

    @Test
    void refund_returnsCreditsReservedButNotSpent() {
        budget.tryConsume(8);
        budget.refund(6);

        assertEquals(6, budget.tryConsume(8));
    }

    @Test
    void timeUntilReset_isZeroWhileCreditsRemainAndCountsDownOnceExhausted() {
        assertEquals(Duration.ZERO, budget.timeUntilReset());

        budget.tryConsume(8);
        clock.advance(Duration.ofSeconds(20));

        assertEquals(40, budget.timeUntilReset().toSeconds());
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
