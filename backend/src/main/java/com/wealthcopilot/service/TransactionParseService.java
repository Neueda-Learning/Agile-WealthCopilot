package com.wealthcopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealthcopilot.dto.response.ParseTransactionResponse;
import com.wealthcopilot.dto.response.SymbolSearchResponse;
import com.wealthcopilot.dto.response.TransactionDraftResponse;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.exception.AiParseFailedException;
import com.wealthcopilot.exception.AiUnavailableException;
import com.wealthcopilot.llm.LlmClient;
import com.wealthcopilot.llm.LlmClientException;
import com.wealthcopilot.llm.LlmMessage;
import com.wealthcopilot.llm.LlmResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * AI Feature 1 — turns free text like "Bought 15 Nvidia at 142 last Tuesday"
 * into a structured draft for the manual form. Never writes to the database;
 * missing details (shares, price, date) come back as a 422 asking the user
 * to add them.
 */
@Service
public class TransactionParseService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You extract stock/ETF transaction details from a user's sentence.
            Today is %s (%s). Respond with ONLY a JSON object, no prose:
            {"ticker": string|null, "side": "BUY"|"SELL"|null, "quantity": number|null,
             "price": number|null, "tradeDate": "YYYY-MM-DD"|null,
             "confidence": "HIGH"|"MEDIUM"|"LOW", "warnings": [string]}
            Rules:
            - Map company names to their primary US ticker (e.g. "Nvidia" -> "NVDA")
              and add a warning noting the mapping.
            - Resolve relative dates ("yesterday", "last Tuesday") against today's
              date and add a warning noting the resolution.
            - Use null for anything the user did not state. NEVER invent a
              quantity, price, or date that is not in the text.
            - quantity and price must be positive numbers when present.
            - If the text is not about recording a buy or sell of a stock or ETF,
              return all-null fields with confidence "LOW".
            """;

    private final LlmClient llmClient;
    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TransactionParseService(
            LlmClient llmClient,
            MarketDataService marketDataService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.llmClient = llmClient;
        this.marketDataService = marketDataService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ParseTransactionResponse parse(String text) {
        JsonNode extraction = extractWithRetry(text);

        List<String> warnings = new ArrayList<>();
        JsonNode modelWarnings = extraction.path("warnings");
        if (modelWarnings.isArray()) {
            modelWarnings.forEach(warning -> warnings.add(warning.asText()));
        }

        TransactionSide side = readSide(extraction);
        BigDecimal quantity = readPositiveDecimal(extraction, "quantity");
        BigDecimal price = readPositiveDecimal(extraction, "price");
        LocalDate tradeDate = readDate(extraction);
        String ticker = extraction.path("ticker").isTextual()
                ? extraction.path("ticker").asText().trim().toUpperCase(Locale.ROOT)
                : null;

        requireCompleteDraft(ticker, side, quantity, price, tradeDate);
        String resolvedTicker = verifyTicker(ticker, warnings);

        if (tradeDate.isAfter(LocalDate.now(clock))) {
            warnings.add("Trade date " + tradeDate + " is in the future — please double-check.");
        }

        String confidence = switch (extraction.path("confidence").asText("MEDIUM")) {
            case "HIGH" -> "HIGH";
            case "LOW" -> "LOW";
            default -> "MEDIUM";
        };

        return new ParseTransactionResponse(
                TransactionDraftResponse.newEntry(resolvedTicker, side, quantity, price, tradeDate),
                confidence,
                List.copyOf(warnings));
    }

    private JsonNode extractWithRetry(String text) {
        LocalDate today = LocalDate.now(clock);
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(
                today,
                today.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)));
        List<LlmMessage> messages = List.of(
                LlmMessage.system(systemPrompt),
                LlmMessage.user(text));

        LlmClientException lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            LlmResult result;
            try {
                result = llmClient.complete(messages, List.of(), true);
            } catch (LlmClientException exception) {
                lastFailure = exception;
                continue;
            }
            JsonNode parsed = tryReadJson(result.content());
            if (parsed != null && parsed.isObject()) {
                return parsed;
            }
        }
        if (lastFailure != null) {
            throw new AiUnavailableException("The AI service is unavailable", lastFailure);
        }
        throw new AiParseFailedException(
                "Could not understand that as a transaction. Try e.g. \"bought 15 NVDA at 142 last Tuesday\".");
    }

    private JsonNode tryReadJson(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "");
        }
        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception exception) {
            return null;
        }
    }

    private TransactionSide readSide(JsonNode extraction) {
        String side = extraction.path("side").asText(null);
        if ("BUY".equalsIgnoreCase(side)) {
            return TransactionSide.BUY;
        }
        if ("SELL".equalsIgnoreCase(side)) {
            return TransactionSide.SELL;
        }
        return null;
    }

    private BigDecimal readPositiveDecimal(JsonNode extraction, String field) {
        JsonNode node = extraction.path(field);
        if (!node.isNumber() && !(node.isTextual() && !node.asText().isBlank())) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(node.asText());
            return value.signum() > 0 ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDate readDate(JsonNode extraction) {
        String date = extraction.path("tradeDate").asText(null);
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (Exception exception) {
            return null;
        }
    }

    /** The "ask for what's missing" behaviour: one 422 listing every gap at once. */
    private void requireCompleteDraft(
            String ticker,
            TransactionSide side,
            BigDecimal quantity,
            BigDecimal price,
            LocalDate tradeDate
    ) {
        List<String> missing = new ArrayList<>();
        if (ticker == null || ticker.isBlank()) {
            missing.add("which stock or ETF (ticker or company name)");
        }
        if (side == null) {
            missing.add("whether you bought or sold");
        }
        if (quantity == null) {
            missing.add("the number of shares");
        }
        if (price == null) {
            missing.add("the price per share");
        }
        if (tradeDate == null) {
            missing.add("the trade date");
        }
        if (!missing.isEmpty()) {
            throw new AiParseFailedException(
                    "Almost there — please also include " + String.join(", ", missing)
                            + ", then parse again.");
        }
    }

    private String verifyTicker(String ticker, List<String> warnings) {
        List<SymbolSearchResponse> matches;
        try {
            matches = marketDataService.search(ticker);
        } catch (RuntimeException exception) {
            // Symbol search being down should not block the draft — saving re-validates.
            warnings.add("Could not verify ticker " + ticker + " right now; it will be checked on save.");
            return ticker;
        }
        boolean known = matches.stream()
                .anyMatch(match -> ticker.equalsIgnoreCase(match.ticker()));
        if (!known) {
            throw new AiParseFailedException(
                    "\"" + ticker + "\" doesn't match a known US ticker. "
                            + "Try the exact symbol (e.g. NVDA) or the full company name.");
        }
        return ticker;
    }
}
