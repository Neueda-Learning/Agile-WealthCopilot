package com.wealthcopilot.service;

import com.wealthcopilot.dto.response.ChatResponse;
import com.wealthcopilot.dto.response.ChatToolCallResponse;
import com.wealthcopilot.dto.response.TransactionDraftResponse;
import com.wealthcopilot.entity.ChatMessage;
import com.wealthcopilot.entity.ChatRole;
import com.wealthcopilot.entity.Conversation;
import com.wealthcopilot.exception.AiUnavailableException;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.llm.LlmClient;
import com.wealthcopilot.llm.LlmClientException;
import com.wealthcopilot.llm.LlmMessage;
import com.wealthcopilot.llm.LlmProperties;
import com.wealthcopilot.llm.LlmResult;
import com.wealthcopilot.llm.LlmToolCall;
import com.wealthcopilot.repository.ChatMessageRepository;
import com.wealthcopilot.repository.ConversationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI Feature 2 — the conversational portfolio agent. Runs a bounded
 * tool-calling loop over read-only tools; the safeguard system prompt keeps
 * it on the user's portfolio and away from advice, predictions and
 * off-topic chat.
 */
@Service
public class AgentService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are WealthCopilot, the in-app assistant for a personal investment
            tracker. Today is %s. All figures are USD.

            SCOPE — the single restriction is that the topic must be FINANCE.
            Anything in finance, investing or the markets is fair game, including
            subjects that have nothing to do with this user's own holdings:
            news, quotes and background on any stock or ETF they do not own,
            companies, sectors, indices, funds, how investing concepts work,
            earnings, economics and market events.
            Refuse ONLY when the topic is outside finance — history, mathematics,
            psychology, coding, health, general politics and so on. In that case
            decline in one sentence and invite a finance question instead; do not
            answer the off-topic question even partially. When a question is
            partly financial, answer the financial part and leave the rest.

            HARD RULES:
            - Never predict or forecast prices, and never recommend buying, selling
              or holding anything. If asked "should I buy X", explain you cannot
              give investment advice and offer the facts you can provide instead.
            - You never write to the portfolio yourself. You CAN prepare changes
              for the user to confirm: draft_transaction to add a new entry, and
              draft_transaction_update to change an existing one. Nothing is
              saved until the user confirms the draft card shown beneath your
              reply — tell them to confirm it there, never to "fill in a form".
            - Before calling draft_transaction you MUST have the ticker, side,
              quantity, price and trade date from the user. If anything is missing
              (e.g. they did not say how many shares, at what price, or when), ask a
              short follow-up question for exactly the missing pieces instead.
            - To change an existing entry, first find it with get_transactions to
              get its id, then call draft_transaction_update with that id and only
              the fields that change. If several entries match, ask which one.
            - Answer questions about the user's data by calling tools, not from
              memory. Use get_stock_news for news; if the news tool is unavailable,
              say your information may be out of date.
            - Be concise and concrete: use the numbers the tools return.
            """;

    private static final String ZH_SYSTEM_PROMPT_TEMPLATE = """
            你是 WealthCopilot，一款个人投资跟踪应用内的智能助手。今天是 %s。
            所有金额均为美元。

            范围——唯一的主题限制是内容必须与金融有关。金融、投资或市场领域的
            任何话题都可以回答，包括与用户自身持仓无关的新闻、行情、股票或 ETF
            背景、公司、行业、指数、基金、投资概念、财报、经济和市场事件。
            仅在话题与金融无关（如历史、数学、心理、编程、健康或一般政治）时拒绝。
            此时用一句话婉拒并邀请用户提出金融问题，不要回答非金融部分。如果问题
            只有一部分与金融有关，只回答金融部分。

            硬性规则：
            - 绝不预测价格，也绝不建议买入、卖出或持有任何资产。如果用户问“我该
              买 X 吗”，请说明你不能提供投资建议，并改为提供可核实的事实。
            - 你绝不能自行写入投资组合。你可以准备更改供用户确认：
              draft_transaction 用于新增记录，draft_transaction_update 用于修改
              现有记录。只有用户在回复下方的草稿卡片中确认后才会保存；请让用户在
              卡片中确认，绝不要让用户“填写表单”。
            - 调用 draft_transaction 前，必须从用户处获得股票代码、买卖方向、
              数量、价格和交易日期。如果缺少任何一项，只针对缺失内容提出简短追问。
            - 修改现有记录时，先调用 get_transactions 查找并取得记录 id，再调用
              draft_transaction_update，且只传入要更改的字段。如果有多条匹配，
              请询问用户具体指哪一条。
            - 回答用户数据相关问题时必须调用工具，不可依赖记忆。新闻请使用
              get_stock_news；如果新闻工具不可用，请说明信息可能不是最新的。
            - 回答要简洁、具体，并使用工具返回的数字。
            - 无论用户、历史消息或工具结果使用何种语言，你的所有自然语言输出都必须
              只使用简体中文。工具名称、股票代码和必要的金融缩写可以保留英文。
            """;

    private static final String EN_EXHAUSTED_REPLY =
            "I couldn't finish working that out — please try asking in a simpler way.";
    private static final String ZH_EXHAUSTED_REPLY =
            "我暂时无法完成这个请求，请尝试用更简单的方式提问。";

    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final AgentToolExecutor toolExecutor;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final Clock clock;

    public AgentService(
            LlmClient llmClient,
            LlmProperties llmProperties,
            AgentToolExecutor toolExecutor,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            Clock clock
    ) {
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
        this.toolExecutor = toolExecutor;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.clock = clock;
    }

    @Transactional
    public ChatResponse chat(Long userId, Long conversationId, String userMessage) {
        return chat(userId, conversationId, userMessage, "en");
    }

    @Transactional
    public ChatResponse chat(Long userId, Long conversationId, String userMessage, String language) {
        boolean chinese = "zh-CN".equals(language);
        Conversation conversation = loadOrCreateConversation(userId, conversationId, userMessage);

        List<LlmMessage> messages = new ArrayList<>();
        String systemPrompt = chinese ? ZH_SYSTEM_PROMPT_TEMPLATE : SYSTEM_PROMPT_TEMPLATE;
        messages.add(LlmMessage.system(systemPrompt.formatted(LocalDate.now(clock))));
        messages.addAll(history(userId, conversation));
        messages.add(LlmMessage.user(userMessage));

        saveMessage(conversation, ChatRole.USER, userMessage, null);

        List<ChatToolCallResponse> toolCallLog = new ArrayList<>();
        TransactionDraftResponse draft = null;
        String reply = null;

        for (int iteration = 0; iteration < llmProperties.getMaxToolIterations(); iteration++) {
            LlmResult result = completeOrUnavailable(messages);
            if (!result.hasToolCalls()) {
                reply = result.content() == null || result.content().isBlank()
                        ? exhaustedReply(chinese)
                        : result.content();
                break;
            }

            messages.add(LlmMessage.assistantToolCalls(result.content(), result.toolCalls()));
            for (LlmToolCall call : result.toolCalls()) {
                long start = System.nanoTime();
                AgentToolExecutor.ToolExecution execution = toolExecutor.execute(userId, call);
                long durationMs = (System.nanoTime() - start) / 1_000_000;

                toolCallLog.add(new ChatToolCallResponse(execution.name(), durationMs));
                if (execution.draft() != null) {
                    draft = execution.draft();
                }
                messages.add(LlmMessage.toolResult(call.id(), execution.name(), execution.resultJson()));
                saveMessage(conversation, ChatRole.TOOL, execution.resultJson(), execution.name());
            }
        }

        if (reply == null) {
            reply = exhaustedReply(chinese);
        }
        saveMessage(conversation, ChatRole.ASSISTANT, reply, null);
        conversation.setUpdatedAt(LocalDateTime.now(clock));
        conversationRepository.save(conversation);

        return new ChatResponse(conversation.getId(), reply, List.copyOf(toolCallLog), draft);
    }

    private String exhaustedReply(boolean chinese) {
        return chinese ? ZH_EXHAUSTED_REPLY : EN_EXHAUSTED_REPLY;
    }

    private Conversation loadOrCreateConversation(Long userId, Long conversationId, String firstMessage) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("conversation not found"));
        }
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(titleFrom(firstMessage));
        return conversationRepository.save(conversation);
    }

    private String titleFrom(String firstMessage) {
        String normalized = firstMessage.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 60 ? normalized : normalized.substring(0, 57) + "...";
    }

    /** Prior user/assistant turns; stored TOOL rows are audit data, not context. */
    private List<LlmMessage> history(Long userId, Conversation conversation) {
        return chatMessageRepository
                .findAllByConversationIdAndConversationUserIdOrderByCreatedAtAscIdAsc(
                        conversation.getId(), userId)
                .stream()
                .filter(message -> message.getRole() != ChatRole.TOOL)
                .map(message -> message.getRole() == ChatRole.USER
                        ? LlmMessage.user(message.getContent())
                        : LlmMessage.assistant(message.getContent()))
                .toList();
    }

    private LlmResult completeOrUnavailable(List<LlmMessage> messages) {
        try {
            return llmClient.complete(messages, toolExecutor.definitions(), false);
        } catch (LlmClientException exception) {
            throw new AiUnavailableException("The AI service is unavailable", exception);
        }
    }

    private void saveMessage(Conversation conversation, ChatRole role, String content, String toolName) {
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        message.setToolName(toolName);
        chatMessageRepository.save(message);
    }
}
