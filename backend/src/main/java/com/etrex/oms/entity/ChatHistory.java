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

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50)
    private ActionType actionType;

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
        NAVIGATE,       // 頁面導航
        CLICK,          // 按鈕點擊
        SUBMIT,         // 表單提交
        OPEN_MODAL,     // 開啟彈窗
        CLOSE_MODAL,    // 關閉彈窗
        OPEN_FAQ,       // 展開FAQ
        API_CALL        // 後端API呼叫（所有業務操作）
    }
}
