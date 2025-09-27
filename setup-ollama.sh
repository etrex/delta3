#!/bin/bash
# Copyright (c) 2025 Etrex Kuo. All rights reserved.

echo "Setting up Ollama and AI models for OMS on macOS..."

# Check if Homebrew is installed
if ! command -v brew &> /dev/null; then
    echo "Error: Homebrew is required. Please install it first:"
    echo '/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'
    exit 1
fi

# Check if Ollama is installed
if ! command -v ollama &> /dev/null; then
    echo "Installing Ollama via Homebrew..."
    brew install ollama
else
    echo "Ollama is already installed"
fi

# Start Ollama service in background
echo "Starting Ollama service..."
ollama serve > /dev/null 2>&1 &
OLLAMA_PID=$!
echo "Ollama started with PID: $OLLAMA_PID"

# Wait for Ollama to be ready
echo "Waiting for Ollama to start..."
sleep 10

# Check if Ollama is responding
for i in {1..5}; do
    if curl -s http://localhost:11434/api/tags > /dev/null; then
        echo "Ollama is ready!"
        break
    else
        echo "Waiting for Ollama... (attempt $i/5)"
        sleep 5
    fi
done

# Pull Qwen 2.5 7B model (recommended for your M4 Pro)
echo "Downloading Qwen 2.5 7B model... (this may take several minutes)"
ollama pull qwen2.5:7b

# Test the model
echo "Testing the model..."
echo "User: 你好，請簡短回應" | ollama run qwen2.5:7b

echo ""
echo "✅ Ollama setup complete!"
echo "📍 Model available at: http://localhost:11434"
echo "🔧 To manage models: ollama list"
echo "🚀 To chat directly: ollama run qwen2.5:7b"
echo ""
echo "Now you can start the OMS system with: ./startup.sh"