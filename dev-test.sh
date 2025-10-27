#!/bin/bash
# Copyright (c) 2025 Etrex Kuo. All rights reserved.
#
# E2E Testing Environment Startup Script
#
# This script starts the backend with 'test' profile to:
# - Disable rate limiting (allow rapid test requests)
# - Use test configurations
# - Optimize for e2e testing

echo "🧪 Starting Order Management System (E2E Test Mode)..."

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

# Check Ollama (optional for e2e tests)
if ! command -v ollama &> /dev/null; then
    echo "⚠️  Ollama not found. Skipping AI features in test mode..."
else
    # Start Ollama if not running
    if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "🤖 Starting Ollama..."
        OLLAMA_KEEP_ALIVE=-1 ollama serve > ollama-test.log 2>&1 &
        sleep 5
    fi
fi

# Start backend with TEST profile
echo "🖥️  Starting backend server (TEST PROFILE)..."
echo "   ⚠️  Rate limiting: DISABLED"
echo "   ⚠️  For e2e testing only"
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=test > ../backend-test.log 2>&1 &
BACKEND_PID=$!

# Wait for backend to start
echo "⏳ Waiting for backend to initialize..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/products > /dev/null 2>&1; then
        echo "✅ Backend ready (test mode)!"
        break
    else
        echo "   Waiting... ($i/30)"
        sleep 2
    fi
done

# Check if backend started successfully
if ! curl -s http://localhost:8080/api/products > /dev/null 2>&1; then
    echo "❌ Backend failed to start. Check logs:"
    echo "   tail -f backend-test.log"
    exit 1
fi

# Start frontend
echo "🎨 Starting frontend server..."
cd ../frontend
npm run dev > ../frontend-test.log 2>&1 &
FRONTEND_PID=$!

echo ""
echo "🎉 E2E Test Environment Started!"
echo ""
echo "📍 Access URLs:"
echo "   🖼️  Frontend:    http://localhost:5173"
echo "   ⚙️  Backend:     http://localhost:8080/api"
echo "   📚 API Docs:     http://localhost:8080/api-docs"
echo "   🗄️  Database:    http://localhost:8080/h2-console"
echo ""
echo "📊 Test Logs:"
echo "   Backend:       backend-test.log"
echo "   Frontend:      frontend-test.log"
echo "   Ollama:        ollama-test.log"
echo ""
echo "🧪 Test Mode Features:"
echo "   ✅ Rate limiting: DISABLED"
echo "   ✅ Test profile:  ACTIVE"
echo "   ✅ Ready for:     Cypress e2e tests"
echo ""
echo "🔐 Test Accounts:"
echo "   👤 Admin:      admin / password123"
echo "   👤 Customer:   customer1 / password123"
echo ""
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo ""
echo "🚀 Run e2e tests with:"
echo "   cd cypress && npm test"
echo ""
echo "Press Ctrl+C to stop all services"

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Stopping E2E test environment..."

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
