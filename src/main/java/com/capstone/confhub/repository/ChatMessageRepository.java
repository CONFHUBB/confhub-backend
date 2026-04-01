package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ChatMessage> findTop20ByUser_IdAndSessionIdOrderByCreatedAtDesc(Integer userId, String sessionId);

    // Rate limiting queries
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.user.id = :userId AND m.role = 'user' AND m.createdAt > :since")
    long countUserMessagesSince(Integer userId, LocalDateTime since);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.user.id = :userId AND m.intent = 'ANALYZE' AND m.createdAt > :since")
    long countAnalysisSince(Integer userId, LocalDateTime since);

    // Cleanup old sessions
    void deleteByCreatedAtBefore(LocalDateTime before);
}
