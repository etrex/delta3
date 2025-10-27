/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limit 註解，用於限制 API 請求頻率
 *
 * 使用範例：
 * @RateLimit(requests = 10, duration = 60)  // 60 秒內最多 10 次請求
 * @RateLimit(requests = 5, duration = 60)   // 60 秒內最多 5 次請求（AI 接口）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 允許的最大請求次數
     */
    int requests() default 60;

    /**
     * 時間窗口（秒）
     */
    int duration() default 60;
}
