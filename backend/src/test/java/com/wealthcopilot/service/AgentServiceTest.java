package com.wealthcopilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.dto.response.ChatResponse;
import com.wealthcopilot.dto.response.TransactionDraftResponse;
import com.wealthcopilot.entity.ChatMessage;
import com.wealthcopilot.entity.ChatRole;
import com.wealthcopilot.entity.Conversation;
import com.wealthcopilot.entity.TransactionSide;
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
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    private static final Long USER_ID = 7L;
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private LlmClient llmClient;

    @Mock
    private AgentToolExecutor toolExecutor;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Captor
    private ArgumentCaptor<List<LlmMessage>> messagesCaptor;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        LlmProperties properties = new LlmProperties();
        properties.setMaxToolIterations(4);
        agentService = new AgentService(
                llmClient, properties, toolExecutor,
                conversationRepository, chatMessageRepository, FIXED_CLOCK);
        lenient().when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(42L);
            }
            return conversation;
        });
    }

    @Test
    void chat_toolCallThenAnswer_returnsReplyAndToolLog() {
        when(llmClient.complete(anyList(), anyList(), eq(false)))
                .thenReturn(new LlmResult(null, List.of(
                        new LlmToolCall("c1", "get_holdings", "{}"))))
                .thenReturn(new LlmResult("Your biggest loser is PYPL.", List.of()));
        when(toolExecutor.execute(eq(USER_ID), any(LlmToolCall.class)))
                .thenReturn(new AgentToolExecutor.ToolExecution("get_holdings", "[]", null));

        ChatResponse response = agentService.chat(USER_ID, null, "Which holding has lost me the most?");

        assertEquals(42L, response.conversationId());
        assertEquals("Your biggest loser is PYPL.", response.reply());
        assertEquals(1, response.toolCalls().size());
        assertEquals("get_holdings", response.toolCalls().get(0).name());
    }

    @Test
    void chat_systemPromptCarriesGuardrails() {
        when(llmClient.complete(messagesCaptor.capture(), anyList(), eq(false)))
                .thenReturn(new LlmResult("Hello!", List.of()));

        agentService.chat(USER_ID, null, "hi");

        LlmMessage system = messagesCaptor.getValue().get(0);
        assertEquals(LlmMessage.Role.SYSTEM, system.role());
        // Finance-wide scope: other people's tickers are allowed, non-finance is not.
        assertTrue(system.content().contains("the topic must be FINANCE"));
        assertTrue(system.content().contains("they do not own"));
        assertTrue(system.content().contains("Refuse ONLY when the topic is outside finance"));
        assertTrue(system.content().contains("Never predict or forecast prices"));
        assertTrue(system.content().contains("never write to the portfolio yourself"));
        assertTrue(system.content().contains("draft_transaction_update"));
        assertTrue(system.content().contains("ask a"));
        assertTrue(system.content().contains(LocalDate.now(FIXED_CLOCK).toString()));
    }

    @Test
    void chat_draftToolSurfacesDraftTransaction() {
        TransactionDraftResponse draft = TransactionDraftResponse.newEntry(
                "NVDA", TransactionSide.BUY, new BigDecimal("15"),
                new BigDecimal("142.0"), LocalDate.of(2026, 7, 21));
        when(llmClient.complete(anyList(), anyList(), eq(false)))
                .thenReturn(new LlmResult(null, List.of(
                        new LlmToolCall("c1", "draft_transaction", "{}"))))
                .thenReturn(new LlmResult("I've prepared the draft — confirm in the form.", List.of()));
        when(toolExecutor.execute(eq(USER_ID), any(LlmToolCall.class)))
                .thenReturn(new AgentToolExecutor.ToolExecution(
                        "draft_transaction", "{\"status\": \"ok\"}", draft));

        ChatResponse response = agentService.chat(USER_ID, null, "record buying 15 nvda at 142 last tuesday");

        assertNotNull(response.draftTransaction());
        assertEquals("NVDA", response.draftTransaction().ticker());
    }

    @Test
    void chat_existingConversationOfOtherUser_notFound() {
        when(conversationRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> agentService.chat(USER_ID, 9L, "hello"));
    }

    @Test
    void chat_persistsUserToolAndAssistantMessages() {
        when(llmClient.complete(anyList(), anyList(), eq(false)))
                .thenReturn(new LlmResult(null, List.of(
                        new LlmToolCall("c1", "get_portfolio_summary", "{}"))))
                .thenReturn(new LlmResult("All good.", List.of()));
        when(toolExecutor.execute(eq(USER_ID), any(LlmToolCall.class)))
                .thenReturn(new AgentToolExecutor.ToolExecution("get_portfolio_summary", "{}", null));

        agentService.chat(USER_ID, null, "how am I doing?");

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, atLeastOnce()).save(saved.capture());
        List<ChatRole> roles = saved.getAllValues().stream().map(ChatMessage::getRole).toList();
        assertEquals(List.of(ChatRole.USER, ChatRole.TOOL, ChatRole.ASSISTANT), roles);
        assertEquals("get_portfolio_summary", saved.getAllValues().get(1).getToolName());
    }

    @Test
    void chat_iterationCapProducesFallbackReply() {
        when(llmClient.complete(anyList(), anyList(), eq(false)))
                .thenReturn(new LlmResult(null, List.of(
                        new LlmToolCall("c1", "get_holdings", "{}"))));
        when(toolExecutor.execute(eq(USER_ID), any(LlmToolCall.class)))
                .thenReturn(new AgentToolExecutor.ToolExecution("get_holdings", "[]", null));

        ChatResponse response = agentService.chat(USER_ID, null, "loop forever");

        assertTrue(response.reply().contains("couldn't finish"));
    }

    @Test
    void chat_providerDown_mapsToUnavailable() {
        when(llmClient.complete(anyList(), anyList(), eq(false)))
                .thenThrow(new LlmClientException("timeout"));

        assertThrows(
                AiUnavailableException.class,
                () -> agentService.chat(USER_ID, null, "hi"));
    }
}
