/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import axios from './axios'

export default {
  login(username: string, password: string) {
    return axios.post('/auth/login', { username, password })
  }
}