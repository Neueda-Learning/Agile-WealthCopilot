package com.wealthcopilot.service;

import com.wealthcopilot.marketdata.MarketDataProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Tracks how many market-data credits this instance has spent in the current
 * minute.
 *
 * <p>Twelve Data bills one credit per symbol on a multi-symbol {@code /quote}
 * call and rejects the whole request with HTTP 429 once the per-minute
 * allowance is gone — a 25-symbol portfolio refresh on the free tier
 * (8 credits/minute) therefore returned zero prices rather than eight. Both
 * the scheduled batch and the user-initiated refresh draw from this shared
 * budget so a request is trimmed to what the plan allows instead of being
 * rejected wholesale.
 */
@Component
public class PriceRefreshBudget {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final MarketDataProperties properties;
    private final Clock clock;

    private Instant windowStart;
    private int spent;

    public PriceRefreshBudget(MarketDataProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Reserves up to {@code requested} credits.
     *
     * @return the number actually granted, which may be fewer than requested and
     *         may be zero when the current minute is already exhausted.
     */
    public synchronized int tryConsume(int requested) {
        if (requested <= 0) {
            return 0;
        }
        rollWindow();
        int granted = Math.min(requested, Math.max(0, limit() - spent));
        spent += granted;
        return granted;
    }

    /** Returns credits reserved by {@link #tryConsume(int)} but never spent. */
    public synchronized void refund(int credits) {
        if (credits > 0) {
            spent = Math.max(0, spent - credits);
        }
    }

    /** How long until the per-minute allowance resets. Zero when credits remain. */
    public synchronized Duration timeUntilReset() {
        rollWindow();
        if (spent < limit()) {
            return Duration.ZERO;
        }
        Duration elapsed = Duration.between(windowStart, Instant.now(clock));
        Duration remaining = WINDOW.minus(elapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private void rollWindow() {
        Instant now = Instant.now(clock);
        if (windowStart == null || !now.isBefore(windowStart.plus(WINDOW))) {
            windowStart = now;
            spent = 0;
        }
    }

    private int limit() {
        return Math.max(0, properties.getCreditsPerMinute());
    }
}
