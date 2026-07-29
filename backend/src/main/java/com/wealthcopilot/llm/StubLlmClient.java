package com.wealthcopilot.llm;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Deterministic offline stand-in so the app can boot and demo without a
 * DeepSeek key. Parse mode does a naive regex extraction; chat mode fetches
 * the portfolio summary once and answers from it.
 */
@Component
@Profile("stub")
public class StubLlmClient implements LlmClient {

    private static final Pattern QUANTITY_TICKER =
            Pattern.compile("(?i)\\b(bought|buy|sold|sell)\\b\\s+(\\d+(?:\\.\\d+)?)?\\s*([A-Za-z.]{1,10})?");
    private static final Pattern PRICE = Pattern.compile("(?i)\\bat\\s+\\$?(\\d+(?:\\.\\d+)?)");

    private final Clock clock;

    public StubLlmClient(Clock clock) {
        this.clock = clock;
    }

    @Override
    public LlmResult complete(List<LlmMessage> messages, List<LlmToolDefinition> tools, boolean jsonMode) {
        if (jsonMode) {
            return new LlmResult(parseTransactionJson(lastUserContent(messages)), List.of());
        }
        boolean hasToolResult = messages.stream()
                .anyMatch(message -> message.role() == LlmMessage.Role.TOOL);
        boolean summaryAvailable = tools.stream()
                .anyMatch(tool -> "get_portfolio_summary".equals(tool.name()));
        if (!hasToolResult && summaryAvailable) {
            return new LlmResult(null, List.of(
                    new LlmToolCall("stub-call-1", "get_portfolio_summary", "{}")));
        }
        String toolResult = messages.stream()
                .filter(message -> message.role() == LlmMessage.Role.TOOL)
                .reduce((first, second) -> second)
                .map(LlmMessage::content)
                .orElse("no data");
        return new LlmResult(
                "Stub answer (no DeepSeek key configured). Portfolio summary: " + toolResult,
                List.of());
    }

    private String lastUserContent(List<LlmMessage> messages) {
        return messages.stream()
                .filter(message -> message.role() == LlmMessage.Role.USER)
                .reduce((first, second) -> second)
                .map(LlmMessage::content)
                .orElse("");
    }

    private String parseTransactionJson(String text) {
        String side = null;
        String quantity = null;
        String ticker = null;
        Matcher matcher = QUANTITY_TICKER.matcher(text);
        if (matcher.find()) {
            String verb = matcher.group(1).toLowerCase(Locale.ROOT);
            side = verb.startsWith("s") ? "SELL" : "BUY";
            quantity = matcher.group(2);
            ticker = matcher.group(3) == null ? null : matcher.group(3).toUpperCase(Locale.ROOT);
        }
        Matcher priceMatcher = PRICE.matcher(text);
        String price = priceMatcher.find() ? priceMatcher.group(1) : null;
        String date = text.toLowerCase(Locale.ROOT).contains("yesterday")
                ? LocalDate.now(clock).minusDays(1).toString()
                : LocalDate.now(clock).toString();

        return """
                {"ticker": %s, "side": %s, "quantity": %s, "price": %s, "tradeDate": %s, \
                "confidence": "LOW", "warnings": ["Parsed by offline stub"]}"""
                .formatted(json(ticker), json(side), quantity, price, json(date));
    }

    private String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
