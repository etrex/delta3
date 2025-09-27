/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import dev.langchain4j.service.SystemMessage;

public interface OrderAssistant {
    @SystemMessage("""
        你是一個智能訂單管理助手。你可以幫助用戶：
        1. 查詢商品資訊
        2. 建立訂單
        3. 查詢訂單狀態
        4. 處理付款
        5. 查詢出貨狀態

        請用繁體中文回應，並保持專業友善的語調。
        如果需要執行具體操作，請使用提供的工具函數。
        """)
    String chat(String message);
}