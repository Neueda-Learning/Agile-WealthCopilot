package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wealthcopilot.dto.response.PortfolioSummaryResponse;
import com.wealthcopilot.dto.response.TransactionResponse;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.entity.TransactionSource;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.llm.LlmToolCall;
import com.wealthcopilot.news.NewsArticle;
import com.wealthcopilot.news.NewsClient;
import com.wealthcopilot.news.NewsClientException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentToolExecutorTest {

    private static final Long USER_ID = 7L;

    @Mock
    private EzioAgentToolService portfolioTools;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private NewsClient newsClient;

    private Cache<String, List<NewsArticle>> newsCache;
    private AgentToolExecutor executor;

    @BeforeEach
    void setUp() {
        newsCache = Caffeine.newBuilder().maximumSize(10).build();
        executor = new AgentToolExecutor(
                portfolioTools, marketDataService, transactionService, newsClient, newsCache,
                new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    void definitions_containAllSpecTools() {
        List<String> names = executor.definitions().stream().map(d -> d.name()).toList();

        assertTrue(names.containsAll(List.of(
                "get_portfolio_summary", "get_holdings", "get_transactions",
                "get_quote", "get_invested_amount", "draft_transaction",
                "draft_transaction_update", "get_stock_news")));
    }

    @Test
    void execute_dispatchesSummaryScopedToUser() {
        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(
                new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("20"),
                new BigDecimal("25"), BigDecimal.ZERO, null, null, "USD", null, false);
        when(portfolioTools.getPortfolioSummary(USER_ID)).thenReturn(summary);

        AgentToolExecutor.ToolExecution execution =
                executor.execute(USER_ID, new LlmToolCall("1", "get_portfolio_summary", "{}"));

        verify(portfolioTools).getPortfolioSummary(USER_ID);
        assertTrue(execution.resultJson().contains("\"totalValue\":100"));
        assertNull(execution.draft());
    }

    @Test
    void execute_getTransactions_parsesFilters() {
        when(portfolioTools.getTransactions(
                USER_ID, "NVDA", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of());

        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "get_transactions",
                "{\"ticker\": \"NVDA\", \"from\": \"2026-06-01\", \"to\": \"2026-07-01\"}"));

        verify(portfolioTools).getTransactions(
                USER_ID, "NVDA", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1));
        assertEquals("[]", execution.resultJson());
    }

    @Test
    void execute_badDateArgument_returnsErrorNotException() {
        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "get_transactions", "{\"from\": \"last week\"}"));

        assertTrue(execution.resultJson().contains("error"));
        verifyNoInteractions(portfolioTools);
    }

    @Test
    void draftTransaction_completeArguments_returnsDraft() {
        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "draft_transaction",
                "{\"ticker\": \"nvda\", \"side\": \"BUY\", \"quantity\": 15, "
                        + "\"price\": 142.0, \"tradeDate\": \"2026-07-21\"}"));

        assertNotNull(execution.draft());
        assertEquals("NVDA", execution.draft().ticker());
        assertEquals(TransactionSide.BUY, execution.draft().side());
        assertEquals(new BigDecimal("15"), execution.draft().quantity());
        assertEquals(LocalDate.of(2026, 7, 21), execution.draft().tradeDate());
        assertTrue(execution.resultJson().contains("nothing saved"));
    }

    @Test
    void draftTransaction_missingQuantity_asksInsteadOfDrafting() {
        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "draft_transaction",
                "{\"ticker\": \"NVDA\", \"side\": \"BUY\", \"price\": 142.0, "
                        + "\"tradeDate\": \"2026-07-21\"}"));

        assertNull(execution.draft());
        assertTrue(execution.resultJson().contains("ask the user"));
        assertTrue(execution.resultJson().contains("quantity"));
    }

    @Test
    void draftTransaction_negativeQuantity_rejected() {
        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "draft_transaction",
                "{\"ticker\": \"NVDA\", \"side\": \"SELL\", \"quantity\": -3, "
                        + "\"price\": 142.0, \"tradeDate\": \"2026-07-21\"}"));

        assertNull(execution.draft());
        assertTrue(execution.resultJson().contains("quantity"));
    }

    private TransactionResponse existingNvdaBuy() {
        return new TransactionResponse(
                42L, "NVDA", "NVIDIA Corp", TransactionSide.BUY,
                new BigDecimal("15"), new BigDecimal("142.00"), BigDecimal.ZERO,
                LocalDate.of(2026, 7, 21), null, TransactionSource.MANUAL, null);
    }

    @Test
    void draftTransactionUpdate_mergesOverridesOntoExistingRow() {
        when(transactionService.getTransaction(USER_ID, 42L)).thenReturn(existingNvdaBuy());

        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "draft_transaction_update",
                "{\"transactionId\": 42, \"quantity\": 20}"));

        assertNotNull(execution.draft());
        assertEquals(42L, execution.draft().transactionId());
        assertEquals(new BigDecimal("20"), execution.draft().quantity());
        // Untouched fields keep their stored values.
        assertEquals("NVDA", execution.draft().ticker());
        assertEquals(TransactionSide.BUY, execution.draft().side());
        assertEquals(new BigDecimal("142.00"), execution.draft().price());
        assertEquals(LocalDate.of(2026, 7, 21), execution.draft().tradeDate());
    }

    @Test
    void draftTransactionUpdate_otherUsersTransaction_isNotFound() {
        when(transactionService.getTransaction(USER_ID, 99L))
                .thenThrow(new ResourceNotFoundException("transaction not found"));

        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "draft_transaction_update", "{\"transactionId\": 99, \"quantity\": 5}"));

        assertNull(execution.draft());
        assertTrue(execution.resultJson().contains("no transaction with id 99"));
    }

    @Test
    void draftTransactionUpdate_missingId_asksForLookup() {
        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "draft_transaction_update", "{\"quantity\": 5}"));

        assertNull(execution.draft());
        assertTrue(execution.resultJson().contains("transactionId is required"));
        verifyNoInteractions(transactionService);
    }

    @Test
    void draftTransactionUpdate_negativeQuantity_rejected() {
        when(transactionService.getTransaction(USER_ID, 42L)).thenReturn(existingNvdaBuy());

        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "draft_transaction_update", "{\"transactionId\": 42, \"quantity\": -1}"));

        assertNull(execution.draft());
        assertTrue(execution.resultJson().contains("quantity must be greater than 0"));
    }

    @Test
    void draftTransactionUpdate_noActualChange_isRejected() {
        when(transactionService.getTransaction(USER_ID, 42L)).thenReturn(existingNvdaBuy());

        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "draft_transaction_update",
                "{\"transactionId\": 42, \"quantity\": 15, \"ticker\": \"nvda\"}"));

        assertNull(execution.draft());
        assertTrue(execution.resultJson().contains("unchanged"));
    }

    @Test
    void draftTransaction_newEntryHasNoTransactionId() {
        AgentToolExecutor.ToolExecution execution = executor.execute(USER_ID, new LlmToolCall(
                "1", "draft_transaction",
                "{\"ticker\": \"NVDA\", \"side\": \"BUY\", \"quantity\": 15, "
                        + "\"price\": 142.0, \"tradeDate\": \"2026-07-21\"}"));

        assertNull(execution.draft().transactionId());
    }

    @Test
    void stockNews_cachesPerTicker() {
        when(newsClient.fetchCompanyNews("NVDA")).thenReturn(List.of(
                new NewsArticle("Headline", "Wire", LocalDateTime.now(), "Summary", "https://x", "Neutral")));

        executor.execute(USER_ID, new LlmToolCall("1", "get_stock_news", "{\"ticker\": \"NVDA\"}"));
        AgentToolExecutor.ToolExecution second =
                executor.execute(USER_ID, new LlmToolCall("2", "get_stock_news", "{\"ticker\": \"NVDA\"}"));

        verify(newsClient).fetchCompanyNews("NVDA");
        assertTrue(second.resultJson().contains("Headline"));
    }

    @Test
    void stockNews_providerDown_degradesToErrorResult() {
        when(newsClient.fetchCompanyNews("NVDA"))
                .thenThrow(new NewsClientException("no key"));

        AgentToolExecutor.ToolExecution execution =
                executor.execute(USER_ID, new LlmToolCall("1", "get_stock_news", "{\"ticker\": \"NVDA\"}"));

        assertTrue(execution.resultJson().contains("unavailable"));
    }

    @Test
    void execute_unknownTool_returnsError() {
        AgentToolExecutor.ToolExecution execution =
                executor.execute(USER_ID, new LlmToolCall("1", "delete_everything", "{}"));

        assertTrue(execution.resultJson().contains("unknown tool"));
    }
}
