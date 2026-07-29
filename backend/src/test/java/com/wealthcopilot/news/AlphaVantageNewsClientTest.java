package com.wealthcopilot.news;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AlphaVantageNewsClientTest {

    private NewsProperties properties;
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private AlphaVantageNewsClient client;

    @BeforeEach
    void setUp() {
        properties = new NewsProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://alphavantage.test");
        properties.setMaxItems(2);

        builder = RestClient.builder().baseUrl("https://alphavantage.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AlphaVantageNewsClient(builder.build(), properties);
    }

    @Test
    void fetchCompanyNews_parsesFeedAndPrefersTickerSentiment() {
        server.expect(once(), queryParam("function", "NEWS_SENTIMENT"))
                .andExpect(queryParam("tickers", "NVDA"))
                .andExpect(queryParam("sort", "LATEST"))
                .andExpect(queryParam("limit", "2"))
                .andExpect(queryParam("apikey", "test-key"))
                .andRespond(withSuccess("""
                        {
                          "items": "2",
                          "feed": [
                            {
                              "title": "NVIDIA beats expectations",
                              "url": "https://news.test/1",
                              "time_published": "20260727T143000",
                              "summary": "Quarterly results.",
                              "source": "Benzinga",
                              "overall_sentiment_label": "Neutral",
                              "ticker_sentiment": [
                                {"ticker": "AMD", "ticker_sentiment_label": "Bearish"},
                                {"ticker": "NVDA", "ticker_sentiment_label": "Somewhat-Bullish"}
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<NewsArticle> articles = client.fetchCompanyNews("nvda");

        assertEquals(1, articles.size());
        NewsArticle article = articles.get(0);
        assertEquals("NVIDIA beats expectations", article.headline());
        assertEquals("Benzinga", article.source());
        assertEquals(LocalDateTime.of(2026, 7, 27, 14, 30, 0), article.publishedAt());
        assertEquals("https://news.test/1", article.url());
        assertEquals("Somewhat-Bullish", article.sentimentLabel());
        server.verify();
    }

    @Test
    void fetchCompanyNews_fallsBackToOverallSentiment() {
        server.expect(once(), queryParam("tickers", "NVDA"))
                .andRespond(withSuccess("""
                        {"feed": [{"title": "Something", "overall_sentiment_label": "Bullish"}]}
                        """, MediaType.APPLICATION_JSON));

        List<NewsArticle> articles = client.fetchCompanyNews("NVDA");

        assertEquals("Bullish", articles.get(0).sentimentLabel());
        assertNull(articles.get(0).publishedAt());
    }

    @Test
    void fetchCompanyNews_honoursMaxItems() {
        server.expect(once(), queryParam("tickers", "NVDA"))
                .andRespond(withSuccess("""
                        {"feed": [
                          {"title": "One"}, {"title": "Two"}, {"title": "Three"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(2, client.fetchCompanyNews("NVDA").size());
    }

    /** Alpha Vantage reports quota exhaustion as HTTP 200 with an Information key. */
    @Test
    void fetchCompanyNews_informationBody_throwsDespiteHttp200() {
        server.expect(once(), queryParam("tickers", "NVDA"))
                .andRespond(withSuccess("""
                        {"Information": "Thank you for using Alpha Vantage! Our standard API rate limit is reached."}
                        """, MediaType.APPLICATION_JSON));

        NewsClientException exception = assertThrows(
                NewsClientException.class,
                () -> client.fetchCompanyNews("NVDA"));

        assertTrue(exception.getMessage().contains("rate limit"));
    }

    @Test
    void fetchCompanyNews_errorMessageBody_throws() {
        server.expect(once(), queryParam("tickers", "NVDA"))
                .andRespond(withSuccess("""
                        {"Error Message": "Invalid API call."}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(NewsClientException.class, () -> client.fetchCompanyNews("NVDA"));
    }

    @Test
    void fetchCompanyNews_redactsApiKeyFromProviderMessage() {
        server.expect(once(), queryParam("tickers", "NVDA"))
                .andRespond(withSuccess(
                        "{\"Information\": \"Invalid apikey=test-key supplied\"}",
                        MediaType.APPLICATION_JSON));

        NewsClientException exception = assertThrows(
                NewsClientException.class,
                () -> client.fetchCompanyNews("NVDA"));

        assertFalse(exception.getMessage().contains("test-key"));
        assertTrue(exception.getMessage().contains("***"));
    }

    @Test
    void fetchCompanyNews_httpError_throwsWithoutLeakingUri() {
        server.expect(once(), queryParam("tickers", "NVDA"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        NewsClientException exception = assertThrows(
                NewsClientException.class,
                () -> client.fetchCompanyNews("NVDA"));

        assertTrue(exception.getMessage().contains("429"));
        assertFalse(exception.getMessage().contains("test-key"));
    }

    @Test
    void fetchCompanyNews_emptyFeed_returnsEmptyList() {
        server.expect(once(), queryParam("tickers", "NVDA"))
                .andRespond(withSuccess("{\"items\": \"0\", \"feed\": []}", MediaType.APPLICATION_JSON));

        assertTrue(client.fetchCompanyNews("NVDA").isEmpty());
    }

    @Test
    void fetchCompanyNews_missingApiKey_throwsBeforeCallingProvider() {
        properties.setApiKey("");

        assertThrows(NewsClientException.class, () -> client.fetchCompanyNews("NVDA"));
        server.verify();
    }
}
