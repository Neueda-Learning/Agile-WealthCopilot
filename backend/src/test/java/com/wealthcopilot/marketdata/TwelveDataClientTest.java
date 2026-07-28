package com.wealthcopilot.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class TwelveDataClientTest {

    private MarketDataProperties properties;
    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new MarketDataProperties();
        properties.setApiKey("test-api-key");
        properties.setMaxAttempts(3);
        properties.setRetryInitialBackoff(Duration.ofMillis(250));
        properties.setMaxRetryAfter(Duration.ofSeconds(5));

        builder = RestClient.builder()
                .baseUrl("https://api.twelvedata.test")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "apikey test-api-key");
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void fetchQuotes_parsesBatchAndSkipsPerSymbolErrors() {
        server.expect(once(), requestTo("https://api.twelvedata.test/quote?symbol=AAPL,MSFT"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "apikey test-api-key"))
                .andRespond(withSuccess(
                        """
                        {
                          "AAPL": {
                            "symbol": "AAPL",
                            "close": "215.4000",
                            "previous_close": "214.0000",
                            "timestamp": 1785225600
                          },
                          "MSFT": {
                            "status": "error",
                            "message": "symbol unavailable"
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        TwelveDataClient client = client(millis -> {
        });
        var quotes = client.fetchQuotes(List.of("AAPL", "MSFT"));

        assertEquals(1, quotes.size());
        assertEquals("AAPL", quotes.get("AAPL").ticker());
        assertEquals("215.4000", quotes.get("AAPL").price().toPlainString());
        assertEquals("214.0000", quotes.get("AAPL").previousClose().toPlainString());
        assertEquals(LocalDateTime.parse("2026-07-28T08:00:00"), quotes.get("AAPL").asOf());
        server.verify();
    }

    @Test
    void searchSymbols_parsesNullableFields() {
        server.expect(once(), requestTo("https://api.twelvedata.test/symbol_search?symbol=nvidia&outputsize=30"))
                .andExpect(queryParam("symbol", "nvidia"))
                .andRespond(withSuccess(
                        """
                        {
                          "status": "ok",
                          "data": [{
                            "symbol": "NVDA",
                            "instrument_name": "NVIDIA Corporation",
                            "exchange": "NASDAQ",
                            "instrument_type": "Common Stock",
                            "currency": null
                          }]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var results = client(millis -> {
        }).searchSymbols("nvidia");

        assertEquals(1, results.size());
        assertEquals("NVDA", results.get(0).ticker());
        assertNull(results.get(0).currency());
        server.verify();
    }

    @Test
    void fetchQuotes_retriesRateLimitAndCapsRetryAfter() {
        server.expect(once(), requestTo("https://api.twelvedata.test/quote?symbol=AAPL"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "60"));
        server.expect(once(), requestTo("https://api.twelvedata.test/quote?symbol=AAPL"))
                .andRespond(withSuccess(
                        """
                        {
                          "symbol": "AAPL",
                          "close": "215.4000",
                          "previous_close": "214.0000",
                          "timestamp": 1785225600
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        List<Long> delays = new ArrayList<>();
        var quotes = client(delays::add).fetchQuotes(List.of("AAPL"));

        assertEquals(1, quotes.size());
        assertEquals(List.of(5000L), delays);
        server.verify();
    }

    @Test
    void fetchQuotes_retriesServerErrorsAtConfiguredBackoff() {
        server.expect(times(3), requestTo("https://api.twelvedata.test/quote?symbol=AAPL"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        List<Long> delays = new ArrayList<>();

        MarketDataClientException exception = assertThrows(
                MarketDataClientException.class,
                () -> client(delays::add).fetchQuotes(List.of("AAPL"))
        );

        assertEquals("Twelve Data request failed with status 503", exception.getMessage());
        assertEquals(List.of(250L, 500L), delays);
        server.verify();
    }

    @Test
    void fetchQuotes_retriesNetworkTimeoutsAndStopsAfterMaxAttempts() {
        server.expect(times(3), requestTo("https://api.twelvedata.test/quote?symbol=AAPL"))
                .andRespond(request -> {
                    throw new ResourceAccessException("simulated timeout");
                });
        List<Long> delays = new ArrayList<>();

        MarketDataClientException exception = assertThrows(
                MarketDataClientException.class,
                () -> client(delays::add).fetchQuotes(List.of("AAPL"))
        );

        assertEquals("Twelve Data request failed", exception.getMessage());
        assertEquals(List.of(250L, 500L), delays);
        server.verify();
    }

    @Test
    void fetchQuotes_skipsQuotesWithRequiredFieldsMissing() {
        server.expect(once(), requestTo("https://api.twelvedata.test/quote?symbol=AAPL"))
                .andRespond(withSuccess(
                        """
                        {
                          "symbol": "AAPL",
                          "close": null,
                          "timestamp": 1785225600
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var quotes = client(millis -> {
        }).fetchQuotes(List.of("AAPL"));

        assertTrue(quotes.isEmpty());
        server.verify();
    }

    @Test
    void searchSymbols_returnsEmptyWithoutCallingProviderForBlankQuery() {
        assertTrue(client(millis -> {
        }).searchSymbols(" ").isEmpty());
        server.verify();
    }

    private TwelveDataClient client(TwelveDataClient.Sleeper sleeper) {
        return new TwelveDataClient(builder.build(), properties, sleeper);
    }
}
