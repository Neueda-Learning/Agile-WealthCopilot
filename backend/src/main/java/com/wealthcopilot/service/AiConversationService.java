package com.wealthcopilot.service;

import com.wealthcopilot.dto.response.ChatMessageResponse;
import com.wealthcopilot.dto.response.ConversationPageResponse;
import com.wealthcopilot.dto.response.ConversationResponse;
import com.wealthcopilot.entity.Conversation;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.repository.ChatMessageRepository;
import com.wealthcopilot.repository.ConversationRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public AiConversationService(
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional(readOnly = true)
    public ConversationPageResponse listConversations(Long userId, int page, int size) {
        Page<Conversation> conversations = conversationRepository
                .findAllByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(page, size));
        return new ConversationPageResponse(
                conversations.getContent().stream()
                        .map(conversation -> new ConversationResponse(
                                conversation.getId(),
                                conversation.getTitle(),
                                conversation.getUpdatedAt()))
                        .toList(),
                conversations.getNumber(),
                conversations.getSize(),
                conversations.getTotalElements(),
                conversations.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(Long userId, Long conversationId) {
        requireOwned(userId, conversationId);
        return chatMessageRepository
                .findAllByConversationIdAndConversationUserIdOrderByCreatedAtAscIdAsc(conversationId, userId)
                .stream()
                .map(message -> new ChatMessageResponse(
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);
        conversationRepository.delete(conversation);
    }

    private Conversation requireOwned(Long userId, Long conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("conversation not found"));
    }
}
