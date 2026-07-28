package com.wealthcopilot.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Profile("!stub")
public class TwelveDataClient implements MarketDataClient {

    private final RestClient restClient;
    private final MarketDataProperties properties;
    private final Sleeper sleeper;

    @Autowired
    public TwelveDataClient(RestClient.Builder builder, MarketDataProperties properties) {
        this(createRestClient(builder, properties), properties, Thread::sleep);
    }

    TwelveDataClient(RestClient restClient, MarketDataProperties properties, Sleeper sleeper) {
        this.restClient = restClient;
        this.properties = properties;
        this.sleeper = sleeper;
    }

    @Override
    public Map<String, MarketQuote> fetchQuotes(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Map.of();
        }

        List<String> normalized = tickers.stream()
                .filter(ticker -> ticker != null && !ticker.isBlank())
                .map(ticker -> ticker.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return Map.of();
        }

        JsonNode response = execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", String.join(",", normalized))
                        .build())
                .retrieve()
                .body(JsonNode.class));

        Map<String, MarketQuote> quotes = new LinkedHashMap<>();
        if (normalized.size() == 1 && response != null && response.has("symbol")) {
            parseQuote(response, normalized.get(0)).ifPresent(quote -> quotes.put(quote.ticker(), quote));
            return quotes;
        }

        for (String ticker : normalized) {
            JsonNode quoteNode = findIgnoreCase(response, ticker);
            if (quoteNode != null && quoteNode.has("data") && quoteNode.get("data").isObject()) {
                quoteNode = quoteNode.get("data");
            }
            parseQuote(quoteNode, ticker).ifPresent(quote -> quotes.put(quote.ticker(), quote));
        }
        return quotes;
    }

    @Override
    public List<SymbolSearchResult> searchSymbols(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        JsonNode response = execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/symbol_search")
                        .queryParam("symbol", query.trim())
                        .queryParam("outputsize", 30)
                        .build())
                .retrieve()
                .body(JsonNode.class));

        JsonNode data = response == null ? null : response.get("data");
        if (data == null || !data.isArray()) {
            return List.of();
        }

        List<SymbolSearchResult> results = new ArrayList<>();
        for (JsonNode item : data) {
            String ticker = text(item, "symbol");
            if (ticker == null) {
                continue;
            }
            results.add(new SymbolSearchResult(
                    ticker,
                    text(item, "instrument_name"),
                    text(item, "exchange"),
                    text(item, "instrument_type"),
                    text(item, "currency")
            ));
        }
        return List.copyOf(results);
    }

    private JsonNode execute(Supplier<JsonNode> request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new MarketDataClientException("Twelve Data API key is not configured");
        }

        int attempts = Math.max(1, properties.getMaxAttempts());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                JsonNode response = request.get();
                if (response != null && "error".equalsIgnoreCase(text(response, "status"))) {
                    throw new MarketDataClientException("Twelve Data returned an error response");
                }
                return response;
            } catch (RestClientResponseException exception) {
                boolean retryable = exception.getStatusCode().value() == 429
                        || exception.getStatusCode().is5xxServerError();
                if (!retryable || attempt == attempts) {
                    throw new MarketDataClientException(
                            "Twelve Data request failed with status " + exception.getStatusCode().value()
                    );
                }
                pause(backoff(attempt, exception.getResponseHeaders()));
            } catch (ResourceAccessException exception) {
                if (attempt == attempts) {
                    throw new MarketDataClientException("Twelve Data request failed", exception);
                }
                pause(backoff(attempt, null));
            } catch (RestClientException exception) {
                throw new MarketDataClientException("Twelve Data request failed", exception);
            }
        }
        throw new MarketDataClientException("Twelve Data request failed");
    }

    private Duration backoff(int attempt, HttpHeaders headers) {
        Duration retryAfter = parseRetryAfter(headers);
        if (retryAfter != null) {
            return min(retryAfter, properties.getMaxRetryAfter());
        }
        long multiplier = 1L << Math.max(0, attempt - 1);
        return min(properties.getRetryInitialBackoff().multipliedBy(multiplier), properties.getMaxRetryAfter());
    }

    private Duration parseRetryAfter(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void pause(Duration duration) {
        try {
            sleeper.sleep(Math.max(0, duration.toMillis()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MarketDataClientException("Twelve Data request interrupted", exception);
        }
    }

    private Optional<MarketQuote> parseQuote(JsonNode node, String fallbackTicker) {
        if (node == null || !node.isObject() || "error".equalsIgnoreCase(text(node, "status"))) {
            return Optional.empty();
        }
        BigDecimal price = decimal(node, "close");
        Long timestamp = longValue(node, "timestamp");
        if (price == null || timestamp == null) {
            return Optional.empty();
        }
        String ticker = Optional.ofNullable(text(node, "symbol"))
                .orElse(fallbackTicker)
                .toUpperCase(Locale.ROOT);
        return Optional.of(new MarketQuote(
                ticker,
                price,
                decimal(node, "previous_close"),
                LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC)
        ));
    }

    private JsonNode findIgnoreCase(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }
        var fields = node.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            if (field.getKey().equalsIgnoreCase(fieldName)) {
                return field.getValue();
            }
        }
        return null;
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static BigDecimal decimal(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long longValue(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull() || !value.canConvertToLong()) {
            return null;
        }
        return value.asLong();
    }

    private static Duration min(Duration left, Duration right) {
        return left.compareTo(right) <= 0 ? left : right;
    }

    private static RestClient createRestClient(RestClient.Builder builder, MarketDataProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(properties.getConnectTimeout())
                        .build()
        );
        requestFactory.setReadTimeout(properties.getReadTimeout());

        RestClient.Builder configured = builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "apikey " + properties.getApiKey());
        }
        return configured.build();
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
