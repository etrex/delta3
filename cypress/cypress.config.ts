import { defineConfig } from 'cypress'
import axios from 'axios'

const API_BASE = 'http://localhost:8080/api'

export default defineConfig({
  e2e: {
    setupNodeEvents(on) {
      on('task', {
        log(message) {
          console.log(message)
          return null
        },

        // 重置資料庫（清空訂單、商品、付款）
        async 'db:reset'() {
          try {
            await axios.post(`${API_BASE}/test/reset`)
            console.log('✅ Database reset successfully')
            return null
          } catch (error: any) {
            console.error('❌ Database reset failed:', error.message)
            throw error
          }
        },

        // 初始化預設用戶（admin, customer1）
        async 'db:init:users'() {
          try {
            const response = await axios.post(`${API_BASE}/test/users/init`)
            console.log('✅', response.data)
            return null
          } catch (error: any) {
            console.error('❌ Users init failed:', error.message)
            throw error
          }
        },

        // 批量創建商品
        async 'db:seed:products'(products: any[]) {
          try {
            const response = await axios.post(`${API_BASE}/test/products/seed`, products)
            console.log('✅', response.data)
            return null
          } catch (error: any) {
            console.error('❌ Products seed failed:', error.message)
            throw error
          }
        },

        // 完整重置：清空 + 初始化用戶
        async 'db:resetAll'() {
          try {
            await axios.post(`${API_BASE}/test/reset`)
            await axios.post(`${API_BASE}/test/users/init`)
            console.log('✅ Database fully reset')
            return null
          } catch (error: any) {
            console.error('❌ Full reset failed:', error.message)
            throw error
          }
        }
      })
    },
    baseUrl: 'http://localhost:5173',
    specPattern: 'e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'support/e2e.ts',
    video: false,
    screenshotOnRunFailure: false,
    viewportWidth: 1280,
    viewportHeight: 720
  },
})