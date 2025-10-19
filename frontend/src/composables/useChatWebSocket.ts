/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/stores/auth'

export interface ChatMessage {
  type: string
  content: string
  messageId?: number
}

// Define subscription interface (StompSubscription is not exported from @stomp/stompjs)
export interface Subscription {
  id: string
  unsubscribe: () => void
}

// Singleton WebSocket client
let client: Client | null = null
let connected = ref(false)
let connectionPromise: Promise<void> | null = null

/**
 * Get or create WebSocket client (Singleton)
 */
function getClient(): Client {
  if (client) {
    return client
  }

  const authStore = useAuthStore()

  const stompClient = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws/chat'),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,

    // Add JWT token to connection headers
    connectHeaders: {
      Authorization: `Bearer ${authStore.token || ''}`
    },

    onConnect: () => {
      console.log('WebSocket connected')
      connected.value = true
    },

    onDisconnect: () => {
      console.log('WebSocket disconnected')
      connected.value = false
    },

    onStompError: (frame) => {
      console.error('STOMP error:', frame)
    }
  })

  client = stompClient
  return stompClient
}

/**
 * Connect to WebSocket server
 * Returns a Promise that resolves when connected
 */
function connect(): Promise<void> {
  // If already connected, resolve immediately
  if (connected.value && client) {
    return Promise.resolve()
  }

  // If connection in progress, return existing promise
  if (connectionPromise) {
    return connectionPromise
  }

  // Create new connection promise
  connectionPromise = new Promise<void>((resolve, reject) => {
    const stompClient = getClient()

    // If already connected, resolve immediately
    if (connected.value) {
      resolve()
      connectionPromise = null
      return
    }

    // Set up one-time connection listener
    const originalOnConnect = stompClient.onConnect
    stompClient.onConnect = (frame) => {
      console.log('WebSocket connection established')
      connected.value = true
      if (originalOnConnect) originalOnConnect(frame)
      resolve()
      connectionPromise = null
    }

    // Set up one-time error listener
    const originalOnStompError = stompClient.onStompError
    stompClient.onStompError = (frame) => {
      console.error('WebSocket connection error:', frame)
      if (originalOnStompError) originalOnStompError(frame)
      reject(new Error('WebSocket connection failed'))
      connectionPromise = null
    }

    // Start connection
    if (!stompClient.active) {
      stompClient.activate()
    }

    // Set timeout
    setTimeout(() => {
      if (!connected.value) {
        reject(new Error('WebSocket connection timeout'))
        connectionPromise = null
      }
    }, 10000)
  })

  return connectionPromise
}

/**
 * Disconnect from WebSocket server
 */
function disconnect() {
  if (client) {
    client.deactivate()
    client = null
    connected.value = false
    connectionPromise = null
  }
}

/**
 * Subscribe to user-specific messages
 * Note: Spring WebSocket automatically routes /user/queue/messages to the current user's session
 * based on the Principal name (userId). No need to include userId in the subscription path.
 */
async function subscribeToUserMessages(
  userId: number,
  callback: (message: ChatMessage) => void
): Promise<Subscription> {
  // Ensure connection is established
  await connect()

  const stompClient = getClient()
  // Spring will automatically route this to /queue/messages-user{sessionId} internally
  const destination = '/user/queue/messages'

  const subscription = stompClient.subscribe(destination, (message) => {
    try {
      const payload: ChatMessage = JSON.parse(message.body)
      console.log(`[User ${userId}] Received message:`, payload)
      callback(payload)
    } catch (error) {
      console.error('Error parsing user message:', error)
    }
  })

  console.log(`[User ${userId}] Subscribed to ${destination}`)
  return subscription
}

/**
 * Subscribe to session-specific updates
 */
async function subscribeToSessionUpdates(
  sessionId: string,
  callback: (message: ChatMessage) => void
): Promise<Subscription> {
  // Ensure connection is established
  await connect()

  const stompClient = getClient()
  const destination = `/topic/session/${sessionId}/updates`

  const subscription = stompClient.subscribe(destination, (message) => {
    try {
      const payload: ChatMessage = JSON.parse(message.body)
      console.log('Received session update:', payload)
      callback(payload)
    } catch (error) {
      console.error('Error parsing session update:', error)
    }
  })

  console.log(`Subscribed to ${destination}`)
  return subscription
}

/**
 * Subscribe to admin new message notifications
 */
async function subscribeToAdminNewMessages(
  callback: (message: any) => void
): Promise<Subscription> {
  // Ensure connection is established
  await connect()

  const stompClient = getClient()
  const destination = '/topic/admin/new-messages'

  const subscription = stompClient.subscribe(destination, (message) => {
    try {
      const payload = JSON.parse(message.body)
      console.log('Received admin new message notification:', payload)
      callback(payload)
    } catch (error) {
      console.error('Error parsing admin notification:', error)
    }
  })

  console.log(`Subscribed to ${destination}`)
  return subscription
}

/**
 * Subscribe to admin AI suggestion notifications
 */
async function subscribeToAdminSuggestions(
  callback: (message: any) => void
): Promise<Subscription> {
  // Ensure connection is established
  await connect()

  const stompClient = getClient()
  const destination = '/topic/admin/suggestions'

  const subscription = stompClient.subscribe(destination, (message) => {
    try {
      const payload = JSON.parse(message.body)
      console.log('Received admin suggestion notification:', payload)
      callback(payload)
    } catch (error) {
      console.error('Error parsing admin suggestion:', error)
    }
  })

  console.log(`Subscribed to ${destination}`)
  return subscription
}

/**
 * Subscribe to admin user action notifications
 */
async function subscribeToAdminUserActions(
  callback: (message: any) => void
): Promise<Subscription> {
  // Ensure connection is established
  await connect()

  const stompClient = getClient()
  const destination = '/topic/admin/user-actions'

  const subscription = stompClient.subscribe(destination, (message) => {
    try {
      const payload = JSON.parse(message.body)
      console.log('Received admin user action notification:', payload)
      callback(payload)
    } catch (error) {
      console.error('Error parsing admin user action:', error)
    }
  })

  console.log(`Subscribed to ${destination}`)
  return subscription
}

/**
 * Composable for WebSocket connection (Singleton pattern)
 */
export function useChatWebSocket() {
  return {
    connected,
    connect,
    disconnect,
    subscribeToUserMessages,
    subscribeToSessionUpdates,
    subscribeToAdminNewMessages,
    subscribeToAdminSuggestions,
    subscribeToAdminUserActions
  }
}
