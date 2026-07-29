package com.wealthcopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.wealthcopilot.dto.response.TransactionDraftResponse;
import com.wealthcopilot.dto.response.TransactionResponse;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.llm.LlmToolCall;
import com.wealthcopilot.llm.LlmToolDefinition;
import com.wealthcopilot.news.NewsArticle;
import com.wealthcopilot.news.NewsClient;
import com.wealthcopilot.news.NewsClientException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The agent's read-only tool registry. Every tool runs through the same
 * user-scoped service methods as the REST API, so the AI cannot cross user
 * boundaries; the only mutation-shaped tool (draft_transaction) returns a
 * draft for UI confirmation and never writes.
 */
@Component
public class AgentToolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentToolExecutor.class);

    private static final String NO_PARAMS_SCHEMA = """
            {"type": "object", "properties": {}, "required": []}""";

    private final EzioAgentToolService portfolioTools;
    private final MarketDataService marketDataService;
    private final TransactionService transactionService;
    private final NewsClient newsClient;
    private final Cache<String, List<NewsArticle>> companyNewsCache;
    private final ObjectMapper objectMapper;

    public AgentToolExecutor(
            EzioAgentToolService portfolioTools,
            MarketDataService marketDataService,
            TransactionService transactionService,
            NewsClient newsClient,
            Cache<String, List<NewsArticle>> companyNewsCache,
            ObjectMapper objectMapper
    ) {
        this.portfolioTools = portfolioTools;
        this.marketDataService = marketDataService;
        this.transactionService = transactionService;
        this.newsClient = newsClient;
        this.companyNewsCache = companyNewsCache;
        this.objectMapper = objectMapper;
    }

    /** Outcome of one tool call; draft is non-null only for a valid draft tool. */
    public record ToolExecution(String name, String resultJson, TransactionDraftResponse draft) {
    }

    public List<LlmToolDefinition> definitions() {
        return List.of(
                new LlmToolDefinition(
                        "get_portfolio_summary",
                        "Current totals for the user's portfolio: total value, cost basis, "
                                + "unrealized and realized P&L, day change.",
                        NO_PARAMS_SCHEMA),
                new LlmToolDefinition(
                        "get_holdings",
                        "Every current holding with quantity, average cost, market value, "
                                + "unrealized P&L and portfolio weight.",
                        NO_PARAMS_SCHEMA),
                new LlmToolDefinition(
                        "get_transactions",
                        "The user's buy/sell history, optionally filtered.",
                        """
                        {"type": "object", "properties": {
                          "ticker": {"type": "string", "description": "filter to one ticker"},
                          "from": {"type": "string", "description": "ISO date, inclusive"},
                          "to": {"type": "string", "description": "ISO date, inclusive"}
                        }, "required": []}"""),
                new LlmToolDefinition(
                        "get_quote",
                        "Latest cached market quote for a ticker.",
                        """
                        {"type": "object", "properties": {
                          "ticker": {"type": "string"}
                        }, "required": ["ticker"]}"""),
                new LlmToolDefinition(
                        "get_invested_amount",
                        "Cash invested, proceeds and realized P&L over a date range.",
                        """
                        {"type": "object", "properties": {
                          "from": {"type": "string", "description": "ISO date, inclusive"},
                          "to": {"type": "string", "description": "ISO date, inclusive"}
                        }, "required": []}"""),
                new LlmToolDefinition(
                        "draft_transaction",
                        "Prepare a buy/sell draft for the user to confirm in the form. "
                                + "Requires ALL fields — if the user has not given the quantity, "
                                + "price or date, ask them first instead of calling this.",
                        """
                        {"type": "object", "properties": {
                          "ticker": {"type": "string"},
                          "side": {"type": "string", "enum": ["BUY", "SELL"]},
                          "quantity": {"type": "number", "exclusiveMinimum": 0},
                          "price": {"type": "number", "exclusiveMinimum": 0},
                          "tradeDate": {"type": "string", "description": "YYYY-MM-DD"}
                        }, "required": ["ticker", "side", "quantity", "price", "tradeDate"]}"""),
                new LlmToolDefinition(
                        "draft_transaction_update",
                        "Prepare a change to one of the user's EXISTING transactions for them "
                                + "to confirm. Find the id with get_transactions first. Pass only "
                                + "the fields that change — anything omitted keeps its current "
                                + "value. Nothing is saved until the user confirms.",
                        """
                        {"type": "object", "properties": {
                          "transactionId": {"type": "integer", "description": "id from get_transactions"},
                          "ticker": {"type": "string"},
                          "side": {"type": "string", "enum": ["BUY", "SELL"]},
                          "quantity": {"type": "number", "exclusiveMinimum": 0},
                          "price": {"type": "number", "exclusiveMinimum": 0},
                          "tradeDate": {"type": "string", "description": "YYYY-MM-DD"}
                        }, "required": ["transactionId"]}"""),
                new LlmToolDefinition(
                        "get_stock_news",
                        "Recent news headlines for a ticker from the last few days. "
                                + "sentimentLabel is the news provider's description of an "
                                + "article's tone — report it as coverage, never as a "
                                + "recommendation or a forecast.",
                        """
                        {"type": "object", "properties": {
                          "ticker": {"type": "string"}
                        }, "required": ["ticker"]}"""));
    }

    public ToolExecution execute(Long userId, LlmToolCall call) {
        JsonNode args = readArguments(call);
        try {
            return switch (call.name()) {
                case "get_portfolio_summary" -> result(call, portfolioTools.getPortfolioSummary(userId));
                case "get_holdings" -> result(call, portfolioTools.getHoldings(userId));
                case "get_transactions" -> result(call, portfolioTools.getTransactions(
                        userId,
                        textArg(args, "ticker"),
                        dateArg(args, "from"),
                        dateArg(args, "to")));
                case "get_quote" -> result(call, marketDataService.getQuote(requireTicker(args)));
                case "get_invested_amount" -> result(call, portfolioTools.getInvestedAmount(
                        userId,
                        dateArg(args, "from"),
                        dateArg(args, "to")));
                case "draft_transaction" -> draftTransaction(call, args);
                case "draft_transaction_update" -> draftTransactionUpdate(userId, call, args);
                case "get_stock_news" -> stockNews(call, args);
                default -> error(call, "unknown tool: " + call.name());
            };
        } catch (IllegalArgumentException exception) {
            return error(call, exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.warn("Agent tool {} failed", call.name(), exception);
            return error(call, "tool failed: " + exception.getMessage());
        }
    }

    private ToolExecution draftTransaction(LlmToolCall call, JsonNode args) {
        String ticker = textArg(args, "ticker");
        String side = textArg(args, "side");
        BigDecimal quantity = decimalArg(args, "quantity");
        BigDecimal price = decimalArg(args, "price");
        LocalDate tradeDate = dateArg(args, "tradeDate");

        StringBuilder missing = new StringBuilder();
        if (ticker == null || ticker.isBlank()) {
            append(missing, "ticker");
        }
        if (!"BUY".equalsIgnoreCase(side == null ? "" : side)
                && !"SELL".equalsIgnoreCase(side == null ? "" : side)) {
            append(missing, "side (BUY or SELL)");
        }
        if (quantity == null || quantity.signum() <= 0) {
            append(missing, "positive quantity");
        }
        if (price == null || price.signum() <= 0) {
            append(missing, "positive price");
        }
        if (tradeDate == null) {
            append(missing, "tradeDate (YYYY-MM-DD)");
        }
        if (missing.length() > 0) {
            return error(call, "cannot draft yet — ask the user for: " + missing);
        }

        TransactionDraftResponse draft = TransactionDraftResponse.newEntry(
                ticker.trim().toUpperCase(Locale.ROOT),
                TransactionSide.valueOf(side.toUpperCase(Locale.ROOT)),
                quantity,
                price,
                tradeDate);
        return new ToolExecution(
                call.name(),
                "{\"status\": \"draft prepared and shown to the user for confirmation — nothing saved yet\"}",
                draft);
    }

    /**
     * Edits are expressed as overrides on the stored row, so the user can say
     * "make it 20 shares" without restating the rest. Loading through the
     * user-scoped service is what stops the agent touching another user's data:
     * an id that is not theirs reads as not-found.
     */
    private ToolExecution draftTransactionUpdate(Long userId, LlmToolCall call, JsonNode args) {
        JsonNode idNode = args.path("transactionId");
        if (!idNode.isIntegralNumber() && !(idNode.isTextual() && idNode.asText().matches("\\d+"))) {
            return error(call, "transactionId is required — find it with get_transactions first");
        }

        TransactionResponse existing;
        try {
            existing = transactionService.getTransaction(userId, idNode.asLong());
        } catch (ResourceNotFoundException exception) {
            return error(call, "no transaction with id " + idNode.asText() + " belongs to this user");
        }

        String side = textArg(args, "side");
        if (side != null
                && !"BUY".equalsIgnoreCase(side)
                && !"SELL".equalsIgnoreCase(side)) {
            return error(call, "side must be BUY or SELL");
        }
        BigDecimal quantity = decimalArg(args, "quantity");
        if (quantity != null && quantity.signum() <= 0) {
            return error(call, "quantity must be greater than 0");
        }
        BigDecimal price = decimalArg(args, "price");
        if (price != null && price.signum() <= 0) {
            return error(call, "price must be greater than 0");
        }
        String ticker = textArg(args, "ticker");
        LocalDate tradeDate = dateArg(args, "tradeDate");

        TransactionDraftResponse draft = new TransactionDraftResponse(
                existing.id(),
                ticker != null ? ticker.trim().toUpperCase(Locale.ROOT) : existing.ticker(),
                side != null ? TransactionSide.valueOf(side.toUpperCase(Locale.ROOT)) : existing.side(),
                quantity != null ? quantity : existing.quantity(),
                price != null ? price : existing.price(),
                tradeDate != null ? tradeDate : existing.tradeDate());

        if (draft.ticker().equalsIgnoreCase(existing.ticker())
                && draft.side() == existing.side()
                && draft.quantity().compareTo(existing.quantity()) == 0
                && draft.price().compareTo(existing.price()) == 0
                && draft.tradeDate().equals(existing.tradeDate())) {
            return error(call, "that change would leave the transaction unchanged — "
                    + "ask the user what should differ");
        }

        return new ToolExecution(
                call.name(),
                "{\"status\": \"update drafted and shown to the user for confirmation — nothing saved yet\"}",
                draft);
    }

    private ToolExecution stockNews(LlmToolCall call, JsonNode args) {
        String ticker = requireTicker(args);
        try {
            List<NewsArticle> articles = companyNewsCache.get(ticker, newsClient::fetchCompanyNews);
            if (articles == null || articles.isEmpty()) {
                return error(call, "no recent news found for " + ticker);
            }
            return result(call, articles);
        } catch (RuntimeException exception) {
            Throwable cause = exception instanceof NewsClientException ? exception : exception.getCause();
            LOGGER.warn("News lookup failed for {}", ticker, cause);
            return error(call, "news provider unavailable — answer from general knowledge "
                    + "and say the information may be out of date");
        }
    }

    private JsonNode readArguments(LlmToolCall call) {
        try {
            return objectMapper.readTree(
                    call.argumentsJson() == null || call.argumentsJson().isBlank()
                            ? "{}"
                            : call.argumentsJson());
        } catch (Exception exception) {
            throw new IllegalArgumentException("invalid tool arguments");
        }
    }

    private String requireTicker(JsonNode args) {
        String ticker = textArg(args, "ticker");
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker is required");
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }

    private String textArg(JsonNode args, String field) {
        JsonNode node = args.path(field);
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }

    private BigDecimal decimalArg(JsonNode args, String field) {
        JsonNode node = args.path(field);
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual() && !node.asText().isBlank()) {
            try {
                return new BigDecimal(node.asText());
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private LocalDate dateArg(JsonNode args, String field) {
        String value = textArg(args, field);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(field + " must be an ISO date (YYYY-MM-DD)");
        }
    }

    private ToolExecution result(LlmToolCall call, Object payload) {
        try {
            return new ToolExecution(call.name(), objectMapper.writeValueAsString(payload), null);
        } catch (Exception exception) {
            return error(call, "could not serialize tool result");
        }
    }

    private ToolExecution error(LlmToolCall call, String message) {
        return new ToolExecution(call.name(), "{\"error\": \"" + message.replace("\"", "'") + "\"}", null);
    }

    private void append(StringBuilder missing, String field) {
        if (missing.length() > 0) {
            missing.append(", ");
        }
        missing.append(field);
    }
}
