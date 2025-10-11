/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history")
@Data
public class ChatHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 20)
    private String role;  // USER, ASSISTANT, SYSTEM

    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType;  // MESSAGE, ACTION

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "action_type", length = 50)
    private String actionType;  // navigate, click, submit

    @Column(name = "action_target")
    private String actionTarget;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Role {
        USER, ASSISTANT, SYSTEM
    }

    public enum MessageType {
        MESSAGE, ACTION
    }

    public enum ActionType {
        NAVIGATE, CLICK, SUBMIT, OPEN_MODAL, CLOSE_MODAL
    }
}
