/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration
 * Configures STOMP over SockJS for real-time bidirectional communication
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for /topic (broadcast) and /queue (point-to-point)
        registry.enableSimpleBroker("/topic", "/queue");

        // Application destination prefix for messages from clients
        registry.setApplicationDestinationPrefixes("/app");

        // User destination prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint at /ws/chat with SockJS fallback
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
