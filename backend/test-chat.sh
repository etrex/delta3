#!/bin/bash

# 測試聊天功能並查看 LLM 日誌

echo "=== 準備發送聊天訊息 ==="

# 1. 先登入取得 token
echo "1. 正在登入..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer1",
    "password": "password123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"\(.*\)"/\1/')

if [ -z "$TOKEN" ]; then
    echo "❌ 登入失敗"
    echo "回應: $LOGIN_RESPONSE"
    exit 1
fi

echo "✅ 登入成功"
echo "Token: ${TOKEN:0:50}..."

# 2. 發送聊天訊息
echo ""
echo "2. 正在發送聊天訊息..."
CHAT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "你好，請簡單問候我",
    "pageContext": {
      "currentPage": "/test"
    }
  }')

echo "✅ 聊天回應:"
echo "$CHAT_RESPONSE" | head -20

# 3. 等待日誌寫入
echo ""
echo "3. 等待 2 秒讓日誌寫入..."
sleep 2

# 4. 查看 LLM 日誌
echo ""
echo "=== LLM Request/Response 日誌 ==="
echo ""
grep -E "(╔════|║ LLM|║ Model|║ Type|║ Content|║ AI Response|║ Token)" logs/application.log | tail -50

echo ""
echo "=== 完整日誌檔案位置 ==="
echo "/Users/etrexkuo/Documents/github/etrex/delta3/backend/logs/application.log"
