/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.security;

import com.etrex.oms.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket Authentication Interceptor
 * Intercepts STOMP CONNECT frames to authenticate users via JWT token
 * and set the user Principal on the WebSocket session
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT token from Authorization header
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    // Extract username from token
                    String username = jwtUtil.extractUsername(token);

                    // Load user details
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // Validate token
                    if (jwtUtil.validateToken(token, userDetails)) {
                        // Get user ID from UserDetails (must be User entity)
                        Long userId = ((User) userDetails).getId();

                        // Create a Principal with userId as name (for convertAndSendToUser routing)
                        Principal userPrincipal = () -> String.valueOf(userId);

                        // Set user Principal on WebSocket session
                        accessor.setUser(userPrincipal);

                        log.info("WebSocket authenticated user: {} (userId: {})", username, userId);
                    } else {
                        log.warn("Invalid JWT token for WebSocket connection");
                    }
                } catch (Exception e) {
                    log.error("WebSocket authentication error", e);
                }
            } else {
                log.warn("WebSocket CONNECT without Authorization header");
            }
        }

        return message;
    }
}
