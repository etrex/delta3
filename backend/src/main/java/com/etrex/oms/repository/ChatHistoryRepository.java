/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.repository;

import com.etrex.oms.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    /**
     * Get chat history by session ID, ordered by time
     */
    List<ChatHistory> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * Get recent N records for a session
     */
    @Query(value = "SELECT * FROM chat_history WHERE session_id = :sessionId " +
                   "ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ChatHistory> findRecentBySessionId(@Param("sessionId") String sessionId,
                                            @Param("limit") int limit);

    /**
     * Get recent actions (not messages) for a session
     */
    @Query(value = "SELECT * FROM chat_history WHERE session_id = :sessionId " +
                   "AND message_type = 'ACTION' ORDER BY created_at DESC LIMIT :limit",
                   nativeQuery = true)
    List<ChatHistory> findRecentActionsBySessionId(@Param("sessionId") String sessionId,
                                                    @Param("limit") int limit);

    /**
     * Get chat history by user ID
     */
    List<ChatHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Get all sessions with their last message
     * Returns: [sessionId, userId, lastMessage, lastMessageTime]
     */
    @Query(value = "SELECT ch.session_id, ch.user_id, ch.content, ch.created_at " +
                   "FROM chat_history ch " +
                   "INNER JOIN (" +
                   "  SELECT session_id, MAX(created_at) as max_time " +
                   "  FROM chat_history " +
                   "  WHERE message_type = 'MESSAGE' " +
                   "  GROUP BY session_id" +
                   ") latest ON ch.session_id = latest.session_id AND ch.created_at = latest.max_time " +
                   "WHERE ch.message_type = 'MESSAGE'",
                   nativeQuery = true)
    List<Object[]> findAllSessionsWithLastMessage();
}
