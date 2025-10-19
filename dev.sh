#!/bin/bash
# Copyright (c) 2025 Etrex Kuo. All rights reserved.

echo "🚀 Starting Order Management System..."

# Check dependencies
echo "📋 Checking dependencies..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is required. Please install Java 17 or later:"
    echo "brew install openjdk@17"
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "📦 Installing Maven..."
    brew install maven
fi

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is required. Please install Node.js:"
    echo "brew install node"
    exit 1
fi

# Check NPM packages
if [ ! -d "frontend/node_modules" ]; then
    echo "📦 Installing frontend dependencies..."
    cd frontend && npm install && cd ..
fi

# Check Ollama
if ! command -v ollama &> /dev/null; then
    echo "⚠️  Ollama not found. Please run: ./setup-ollama.sh"
    echo "🔄 Continuing without AI features..."
else
    # Start Ollama if not running
    if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "🤖 Starting Ollama with debug mode..."
        echo "   (KV cache statistics will be logged)"
        OLLAMA_DEBUG=1 OLLAMA_KEEP_ALIVE=-1 ollama serve > ollama.log 2>&1 &
        sleep 5
    fi
fi

# Start backend
echo "🖥️  Starting backend server..."
cd backend
mvn spring-boot:run &
BACKEND_PID=$!

# Wait for backend to start
echo "⏳ Waiting for backend to initialize..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/products > /dev/null 2>&1; then
        echo "✅ Backend ready!"
        break
    else
        echo "   Waiting... ($i/30)"
        sleep 2
    fi
done

# Start frontend
echo "🎨 Starting frontend server..."
cd ../frontend
npm run dev &
FRONTEND_PID=$!

echo ""
echo "🎉 System started successfully!"
echo ""
echo "📍 Access URLs:"
echo "   🖼️  Frontend:    http://localhost:5173"
echo "   ⚙️  Backend:     http://localhost:8080/api"
echo "   📚 API Docs:     http://localhost:8080/api-docs"
echo "   📖 Swagger UI:   http://localhost:8080/swagger-ui.html"
echo "   🗄️  Database:    http://localhost:8080/h2-console"
echo "   💬 Chat API:     http://localhost:8080/api/chat"
echo "   🔌 WebSocket:    ws://localhost:8080/ws/chat"
echo "   🤖 AI Service:   http://localhost:11434"
echo ""
echo "📊 Debug Logs:"
echo "   Backend:       backend/logs/application.log"
echo "   Ollama:        ollama.log (KV cache stats: grep 'used=' ollama.log)"
echo ""
echo "💡 Tips:"
echo "   View backend logs: tail -f backend/logs/application.log"
echo "   View LLM traces:   tail -f backend/logs/application.log | grep TRACE"
echo ""
echo "🔐 Test Accounts:"
echo "   👤 Admin:      admin / password (AI chat with tools, manage suggestions)"
echo "   👤 Customer:   customer1 / password (AI-assisted chat, auto/suggested responses)"
echo ""
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo ""
echo "Press Ctrl+C to stop all services"

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Stopping services..."

    # Kill frontend
    if [ ! -z "$FRONTEND_PID" ]; then
        echo "   Stopping frontend (PID: $FRONTEND_PID)..."
        kill $FRONTEND_PID 2>/dev/null || true
    fi

    # Kill backend and its child processes
    if [ ! -z "$BACKEND_PID" ]; then
        echo "   Stopping backend (PID: $BACKEND_PID)..."
        # Kill the Maven process and all its children
        pkill -P $BACKEND_PID 2>/dev/null || true
        kill $BACKEND_PID 2>/dev/null || true
        # Also kill any process on port 8080
        lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    fi

    echo "✅ All services stopped"
    exit 0
}

# Trap Ctrl+C and other termination signals
trap cleanup SIGINT SIGTERM

# Keep the script running
wait