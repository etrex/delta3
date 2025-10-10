/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import axios from './axios'

export default {
  // Customer chat (default)
  sendMessage(message: string) {
    return axios.post('/chat', { message })
  },

  // Admin chat
  sendAdminMessage(message: string) {
    return axios.post('/chat/admin', { message })
  }
}