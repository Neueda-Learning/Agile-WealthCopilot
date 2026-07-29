package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealthcopilot.dto.response.ParseTransactionResponse;
import com.wealthcopilot.dto.response.SymbolSearchResponse;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.exception.AiParseFailedException;
import com.wealthcopilot.exception.AiUnavailableException;
import com.wealthcopilot.llm.LlmClient;
import com.wealthcopilot.llm.LlmClientException;
import com.wealthcopilot.llm.LlmResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionParseServiceTest {

    // 2026-07-27 is a Monday.
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private LlmClient llmClient;

    @Mock
    private MarketDataService marketDataService;

    private TransactionParseService parseService;

    @BeforeEach
    void setUp() {
        parseService = new TransactionParseService(
                llmClient, marketDataService, new ObjectMapper(), FIXED_CLOCK);
    }

    private void givenLlmReturns(String json) {
        when(llmClient.complete(anyList(), anyList(), eq(true)))
                .thenReturn(new LlmResult(json, List.of()));
    }

    private void givenTickerKnown(String ticker) {
        when(marketDataService.search(ticker)).thenReturn(List.of(
                new SymbolSearchResponse(ticker, "Some Corp", "NASDAQ", InstrumentType.STOCK, "USD")));
    }

    @Test
    void parse_returnsCompleteDraft() {
        givenLlmReturns("""
                {"ticker": "NVDA", "side": "BUY", "quantity": 15, "price": 142.0,
                 "tradeDate": "2026-07-21", "confidence": "HIGH",
                 "warnings": ["Resolved 'Nvidia' to NVDA"]}""");
        givenTickerKnown("NVDA");

        ParseTransactionResponse response = parseService.parse("Bought 15 Nvidia at 142 last Tuesday");

        assertEquals("NVDA", response.draft().ticker());
        assertEquals(TransactionSide.BUY, response.draft().side());
        assertEquals(new BigDecimal("15"), response.draft().quantity());
        assertEquals(new BigDecimal("142.0"), response.draft().price());
        assertEquals(LocalDate.of(2026, 7, 21), response.draft().tradeDate());
        assertEquals("HIGH", response.confidence());
        assertTrue(response.warnings().contains("Resolved 'Nvidia' to NVDA"));
    }

    @Test
    void parse_missingQuantityAndDate_asksForThem() {
        givenLlmReturns("""
                {"ticker": "NVDA", "side": "BUY", "quantity": null, "price": 142.0,
                 "tradeDate": null, "confidence": "MEDIUM", "warnings": []}""");

        AiParseFailedException exception = assertThrows(
                AiParseFailedException.class,
                () -> parseService.parse("Bought some Nvidia at 142"));

        assertTrue(exception.getMessage().contains("number of shares"));
        assertTrue(exception.getMessage().contains("trade date"));
    }

    @Test
    void parse_chineseLocaleReturnsChineseMissingFieldMessage() {
        givenLlmReturns("""
                {"ticker": "NVDA", "side": "BUY", "quantity": null, "price": 142.0,
                 "tradeDate": null, "confidence": "MEDIUM", "warnings": []}""");

        AiParseFailedException exception = assertThrows(
                AiParseFailedException.class,
                () -> parseService.parse("以 142 美元买入 NVDA", "zh-CN"));

        assertTrue(exception.getMessage().contains("股数"));
        assertTrue(exception.getMessage().contains("交易日期"));
    }

    @Test
    void parse_unknownTicker_fails() {
        givenLlmReturns("""
                {"ticker": "NOPE", "side": "BUY", "quantity": 5, "price": 10,
                 "tradeDate": "2026-07-20", "confidence": "HIGH", "warnings": []}""");
        when(marketDataService.search("NOPE")).thenReturn(List.of());

        AiParseFailedException exception = assertThrows(
                AiParseFailedException.class,
                () -> parseService.parse("Bought 5 NOPE at 10 on 2026-07-20"));

        assertTrue(exception.getMessage().contains("NOPE"));
    }

    @Test
    void parse_retriesOnceOnInvalidJsonThenSucceeds() {
        when(llmClient.complete(anyList(), anyList(), eq(true)))
                .thenReturn(new LlmResult("not json at all", List.of()))
                .thenReturn(new LlmResult("""
                        {"ticker": "AAPL", "side": "SELL", "quantity": 2, "price": 210,
                         "tradeDate": "2026-07-24", "confidence": "MEDIUM", "warnings": []}""",
                        List.of()));
        givenTickerKnown("AAPL");

        ParseTransactionResponse response = parseService.parse("Sold 2 AAPL at 210 on Friday");

        assertEquals("AAPL", response.draft().ticker());
        verify(llmClient, times(2)).complete(anyList(), anyList(), eq(true));
    }

    @Test
    void parse_invalidJsonTwice_failsWithParseError() {
        when(llmClient.complete(anyList(), anyList(), eq(true)))
                .thenReturn(new LlmResult("garbage", List.of()));

        assertThrows(AiParseFailedException.class, () -> parseService.parse("blah"));
        verify(llmClient, times(2)).complete(anyList(), anyList(), eq(true));
    }

    @Test
    void parse_providerDown_mapsToUnavailable() {
        when(llmClient.complete(anyList(), anyList(), anyBoolean()))
                .thenThrow(new LlmClientException("boom"));

        assertThrows(AiUnavailableException.class, () -> parseService.parse("Bought 1 AAPL at 1 today"));
    }

    @Test
    void parse_futureDate_addsWarning() {
        givenLlmReturns("""
                {"ticker": "AAPL", "side": "BUY", "quantity": 1, "price": 210,
                 "tradeDate": "2026-08-15", "confidence": "HIGH", "warnings": []}""");
        givenTickerKnown("AAPL");

        ParseTransactionResponse response = parseService.parse("Buying 1 AAPL at 210 on Aug 15");

        assertTrue(response.warnings().stream().anyMatch(warning -> warning.contains("future")));
    }

    @Test
    void parse_stripsMarkdownCodeFences() {
        givenLlmReturns("""
                ```json
                {"ticker": "AAPL", "side": "BUY", "quantity": 1, "price": 210,
                 "tradeDate": "2026-07-27", "confidence": "HIGH", "warnings": []}
                ```""");
        givenTickerKnown("AAPL");

        ParseTransactionResponse response = parseService.parse("Bought 1 AAPL at 210 today");

        assertEquals("AAPL", response.draft().ticker());
    }
}
