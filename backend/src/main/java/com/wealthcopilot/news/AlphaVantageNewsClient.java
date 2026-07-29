package com.wealthcopilot.news;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Alpha Vantage NEWS_SENTIMENT. Note that this API signals errors and quota
 * exhaustion with HTTP 200 plus an "Information"/"Note"/"Error Message" key,
 * so the body — not the status code — decides success.
 */
@Component
@Profile("!stub")
public class AlphaVantageNewsClient implements NewsClient {

    private static final DateTimeFormatter REQUEST_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm", Locale.ROOT);
    private static final DateTimeFormatter PUBLISHED_WITH_SECONDS =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.ROOT);
    private static final List<String> ERROR_FIELDS =
            List.of("Information", "Note", "Error Message");

    private final RestClient restClient;
    private final NewsProperties properties;

    @Autowired
    public AlphaVantageNewsClient(RestClient.Builder builder, NewsProperties properties) {
        this(createRestClient(builder, properties), properties);
    }

    AlphaVantageNewsClient(RestClient restClient, NewsProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    private static RestClient createRestClient(RestClient.Builder builder, NewsProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<NewsArticle> fetchCompanyNews(String ticker) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new NewsClientException("Alpha Vantage API key is not configured");
        }
        String symbol = ticker.trim().toUpperCase(Locale.ROOT);
        String timeFrom = LocalDateTime.now()
                .minusDays(properties.getLookbackDays())
                .format(REQUEST_TIME);

        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "NEWS_SENTIMENT")
                            .queryParam("tickers", symbol)
                            .queryParam("time_from", timeFrom)
                            .queryParam("sort", "LATEST")
                            .queryParam("limit", Math.max(properties.getMaxItems(), 1))
                            .queryParam("apikey", properties.getApiKey())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw new NewsClientException(
                    "Alpha Vantage request failed with status " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            // Message omitted: it can echo the request URI, which carries the API key.
            throw new NewsClientException("Alpha Vantage request failed");
        }

        if (response == null) {
            return List.of();
        }
        for (String field : ERROR_FIELDS) {
            if (response.path(field).isTextual()) {
                throw new NewsClientException(
                        "Alpha Vantage: " + redact(response.path(field).asText()));
            }
        }

        JsonNode feed = response.path("feed");
        if (!feed.isArray()) {
            return List.of();
        }

        List<NewsArticle> articles = new ArrayList<>();
        for (JsonNode item : feed) {
            if (articles.size() >= properties.getMaxItems()) {
                break;
            }
            String headline = text(item, "title");
            if (headline == null) {
                continue;
            }
            articles.add(new NewsArticle(
                    headline,
                    text(item, "source"),
                    parsePublishedAt(text(item, "time_published")),
                    text(item, "summary"),
                    text(item, "url"),
                    sentimentLabel(item, symbol)));
        }
        return List.copyOf(articles);
    }

    /**
     * Prefers the sentiment scoped to the ticker we asked about; an article can
     * mention several symbols and rate them differently.
     */
    private String sentimentLabel(JsonNode item, String symbol) {
        JsonNode tickerSentiment = item.path("ticker_sentiment");
        if (tickerSentiment.isArray()) {
            for (JsonNode entry : tickerSentiment) {
                if (symbol.equalsIgnoreCase(entry.path("ticker").asText(null))) {
                    String label = text(entry, "ticker_sentiment_label");
                    if (label != null) {
                        return label;
                    }
                }
            }
        }
        return text(item, "overall_sentiment_label");
    }

    private LocalDateTime parsePublishedAt(String value) {
        if (value == null) {
            return null;
        }
        for (DateTimeFormatter formatter : List.of(PUBLISHED_WITH_SECONDS, REQUEST_TIME)) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
                // try the next known layout
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    /** Provider messages are echoed to logs; never let the key ride along. */
    private String redact(String message) {
        String apiKey = properties.getApiKey();
        return apiKey == null || apiKey.isBlank()
                ? message
                : message.replace(apiKey, "***");
    }
}
