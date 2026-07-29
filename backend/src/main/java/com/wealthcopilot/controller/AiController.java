package com.wealthcopilot.controller;

import com.wealthcopilot.dto.request.AiChatRequest;
import com.wealthcopilot.dto.request.AiParseRequest;
import com.wealthcopilot.dto.response.ChatMessageResponse;
import com.wealthcopilot.dto.response.ChatResponse;
import com.wealthcopilot.dto.response.ConversationPageResponse;
import com.wealthcopilot.dto.response.ParseTransactionResponse;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.service.AgentService;
import com.wealthcopilot.service.AiConversationService;
import com.wealthcopilot.service.TransactionParseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final TransactionParseService transactionParseService;
    private final AgentService agentService;
    private final AiConversationService conversationService;

    public AiController(
            TransactionParseService transactionParseService,
            AgentService agentService,
            AiConversationService conversationService
    ) {
        this.transactionParseService = transactionParseService;
        this.agentService = agentService;
        this.conversationService = conversationService;
    }

    /** AI Feature 1 — returns a draft for confirmation; never writes. */
    @PostMapping("/parse-transaction")
    public ParseTransactionResponse parseTransaction(@Valid @RequestBody AiParseRequest request) {
        return transactionParseService.parse(request.text(), request.language());
    }

    /** AI Feature 2 — read-only agent; omit conversationId to start a new one. */
    @PostMapping("/chat")
    public ChatResponse chat(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody AiChatRequest request
    ) {
        return agentService.chat(userId, request.conversationId(), request.message(), request.language());
    }

    @GetMapping("/conversations")
    public ConversationPageResponse listConversations(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new DomainValidationException("page must be non-negative and size must be between 1 and 100");
        }
        return conversationService.listConversations(userId, page, size);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<ChatMessageResponse> listMessages(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id
    ) {
        return conversationService.listMessages(userId, id);
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id
    ) {
        conversationService.deleteConversation(userId, id);
    }
}
