/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.interceptor;

import com.etrex.oms.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Rate Limiting Interceptor
 * 簡單的基於內存的流量限制攔截器（Demo 專案用）
 *
 * 算法：滑動窗口（Sliding Window）
 * 存儲：ConcurrentHashMap（單機內存）
 *
 * 注意：此實現僅適用於單機部署的 Demo 專案
 * 生產環境建議使用 Redis + Lua 腳本實現分布式限流
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Environment environment;

    // 存儲每個用戶的請求時間戳隊列
    // Key: userId_methodName, Value: 請求時間戳隊列
    private final Map<String, Queue<Long>> requestRecords = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        // 🧪 測試環境：停用 Rate Limiting（避免影響 e2e 測試）
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            log.debug("Rate limiting disabled in test environment");
            return true;
        }

        // 只處理有 @RateLimit 註解的方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return true;  // 沒有 @RateLimit 註解，直接放行
        }

        // 獲取限流配置
        int maxRequests = rateLimit.requests();
        int durationSeconds = rateLimit.duration();

        // 獲取用戶標識（優先使用 userId，否則用 IP）
        String key = getUserKey(request, handlerMethod);

        // 清理過期的請求記錄
        long now = System.currentTimeMillis();
        long windowStart = now - (durationSeconds * 1000L);

        Queue<Long> timestamps = requestRecords.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        // 移除窗口外的時間戳
        timestamps.removeIf(timestamp -> timestamp < windowStart);

        // 檢查是否超過限制
        if (timestamps.size() >= maxRequests) {
            log.warn("Rate limit exceeded for key: {}, limit: {} requests per {} seconds",
                    key, maxRequests, durationSeconds);

            response.setStatus(429);  // HTTP 429 Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                    "{\"error\": \"請求過於頻繁\", \"message\": \"每 %d 秒最多 %d 次請求，請稍後再試\"}",
                    durationSeconds, maxRequests
            ));
            return false;
        }

        // 記錄本次請求
        timestamps.offer(now);

        return true;
    }

    /**
     * 獲取用戶唯一標識
     * 優先級：用戶 ID > IP 地址
     */
    private String getUserKey(HttpServletRequest request, HandlerMethod handlerMethod) {
        String methodName = handlerMethod.getMethod().getName();

        // 嘗試從 Security Context 獲取用戶 ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            return username + "_" + methodName;
        }

        // 否則使用 IP 地址（用於登錄等未認證接口）
        String ipAddress = getClientIp(request);
        return ipAddress + "_" + methodName;
    }

    /**
     * 獲取真實 IP 地址
     * 考慮反向代理的情況
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多個 IP，取第一個
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
