/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import axios from './axios'

export default {
  sendMessage(message: string) {
    return axios.post('/chat/message', { message })
  },

  chatWithAssistant(message: string) {
    return axios.post('/chat/assistant', { message })
  }
}