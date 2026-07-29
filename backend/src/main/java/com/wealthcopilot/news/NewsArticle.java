package com.wealthcopilot.news;

import java.time.LocalDateTime;

/**
 * @param sentimentLabel the provider's descriptive classification of the
 *                       article's tone (e.g. "Neutral", "Somewhat-Bullish").
 *                       Reporting about coverage — never a recommendation.
 */
public record NewsArticle(
        String headline,
        String source,
        LocalDateTime publishedAt,
        String summary,
        String url,
        String sentimentLabel
) {
}
