package com.wealthcopilot.repository;

import com.wealthcopilot.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** User scoping is transitive: conversation_id -> conversations.user_id. */
    List<ChatMessage> findAllByConversationIdAndConversationUserIdOrderByCreatedAtAscIdAsc(
            Long conversationId,
            Long userId
    );
}
