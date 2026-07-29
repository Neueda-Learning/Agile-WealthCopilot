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

    private static final String ZH_SYSTEM_PROMPT_TEMPLATE = """
            你需要从用户的句子中提取股票或 ETF 交易信息。
            今天是 %s（%s）。仅返回一个 JSON 对象，不要输出任何说明文字：
            {"ticker": string|null, "side": "BUY"|"SELL"|null, "quantity": number|null,
             "price": number|null, "tradeDate": "YYYY-MM-DD"|null,
             "confidence": "HIGH"|"MEDIUM"|"LOW", "warnings": [string]}
            规则：
            - 将公司名称映射为其主要美国股票代码（例如“Nvidia”映射为“NVDA”），
              并在 warnings 中用简体中文说明该映射。
            - 根据今天的日期解析相对日期（如“昨天”“上周二”），并在 warnings
              中用简体中文说明解析结果。
            - 用户没有明确提供的内容必须使用 null。绝不能猜测原文中没有出现的
              数量、价格或日期。
            - quantity 和 price 如果存在，必须是正数。
            - 如果文本不是在记录股票或 ETF 的买入或卖出交易，则所有字段返回 null，
              confidence 返回 "LOW"。
            - warnings 中的所有自然语言内容必须只使用简体中文。
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
        return parse(text, "en");
    }

    public ParseTransactionResponse parse(String text, String language) {
        boolean chinese = "zh-CN".equals(language);
        JsonNode extraction = extractWithRetry(text, chinese);

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

        requireCompleteDraft(ticker, side, quantity, price, tradeDate, chinese);
        String resolvedTicker = verifyTicker(ticker, warnings, chinese);

        if (tradeDate.isAfter(LocalDate.now(clock))) {
            warnings.add(chinese
                    ? "交易日期 " + tradeDate + " 在未来，请再次确认。"
                    : "Trade date " + tradeDate + " is in the future — please double-check.");
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

    private JsonNode extractWithRetry(String text, boolean chinese) {
        LocalDate today = LocalDate.now(clock);
        String promptTemplate = chinese ? ZH_SYSTEM_PROMPT_TEMPLATE : SYSTEM_PROMPT_TEMPLATE;
        Locale promptLocale = chinese ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
        String systemPrompt = promptTemplate.formatted(
                today,
                today.format(DateTimeFormatter.ofPattern("EEEE", promptLocale)));
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
                chinese
                        ? "无法将这段话识别为交易。请尝试输入，例如“上周二以 142 美元买入 15 股 NVDA”。"
                        : "Could not understand that as a transaction. Try e.g. \"bought 15 NVDA at 142 last Tuesday\".");
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
            LocalDate tradeDate,
            boolean chinese
    ) {
        List<String> missing = new ArrayList<>();
        if (ticker == null || ticker.isBlank()) {
            missing.add(chinese ? "股票或 ETF（代码或公司名称）" : "which stock or ETF (ticker or company name)");
        }
        if (side == null) {
            missing.add(chinese ? "买入还是卖出" : "whether you bought or sold");
        }
        if (quantity == null) {
            missing.add(chinese ? "股数" : "the number of shares");
        }
        if (price == null) {
            missing.add(chinese ? "每股价格" : "the price per share");
        }
        if (tradeDate == null) {
            missing.add(chinese ? "交易日期" : "the trade date");
        }
        if (!missing.isEmpty()) {
            throw new AiParseFailedException(
                    chinese
                            ? "快完成了，请补充" + String.join("、", missing) + "，然后重新解析。"
                            : "Almost there — please also include " + String.join(", ", missing)
                                    + ", then parse again.");
        }
    }

    private String verifyTicker(String ticker, List<String> warnings, boolean chinese) {
        List<SymbolSearchResponse> matches;
        try {
            matches = marketDataService.search(ticker);
        } catch (RuntimeException exception) {
            // Symbol search being down should not block the draft — saving re-validates.
            warnings.add(chinese
                    ? "目前无法验证股票代码 " + ticker + "，保存时会再次检查。"
                    : "Could not verify ticker " + ticker + " right now; it will be checked on save.");
            return ticker;
        }
        boolean known = matches.stream()
                .anyMatch(match -> ticker.equalsIgnoreCase(match.ticker()));
        if (!known) {
            throw new AiParseFailedException(
                    chinese
                            ? "“" + ticker + "”与已知的美国股票代码不匹配。"
                                    + "请尝试输入准确代码（如 NVDA）或完整公司名称。"
                            : "\"" + ticker + "\" doesn't match a known US ticker. "
                                    + "Try the exact symbol (e.g. NVDA) or the full company name.");
        }
        return ticker;
    }
}
