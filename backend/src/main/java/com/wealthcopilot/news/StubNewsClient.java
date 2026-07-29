package com.wealthcopilot.news;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("stub")
public class StubNewsClient implements NewsClient {

    private final Clock clock;

    public StubNewsClient(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<NewsArticle> fetchCompanyNews(String ticker) {
        String symbol = ticker.trim().toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now(clock);
        return List.of(
                new NewsArticle(
                        symbol + " announces quarterly results (stub)",
                        "StubWire",
                        now.minusHours(4),
                        "Offline stub article for local development.",
                        "https://example.com/news/1",
                        "Neutral"),
                new NewsArticle(
                        symbol + " expands product line (stub)",
                        "StubWire",
                        now.minusDays(1),
                        "Offline stub article for local development.",
                        "https://example.com/news/2",
                        "Neutral"));
    }
}
