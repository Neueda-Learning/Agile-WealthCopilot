package com.wealthcopilot.service;

import com.wealthcopilot.marketdata.MarketDataProperties;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class MarketHours {

    private static final LocalTime OPEN = LocalTime.of(9, 30);
    private static final LocalTime CLOSE = LocalTime.of(16, 0);

    private final MarketDataProperties properties;
    private final Clock clock;

    public MarketHours(MarketDataProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean isOpen() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(properties.getMarketZone());
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        return !time.isBefore(OPEN) && time.isBefore(CLOSE);
    }
}
