/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import axios from './axios'

export interface Faq {
  id: number
  question: string
  answer: string
  category: string
  createdAt: string
}

export default {
  // Get all FAQs or filter by category
  getFaqs(category?: string): Promise<Faq[]> {
    const params = category ? { category } : {}
    return axios.get('/faqs', { params })
  },

  // Get all categories
  getCategories(): Promise<string[]> {
    return axios.get('/faqs/categories')
  },

  // Search FAQs by keyword
  searchFaqs(keyword: string): Promise<Faq[]> {
    return axios.get('/faqs/search', { params: { keyword } })
  },

  // Get single FAQ by ID
  getFaqById(id: number): Promise<Faq> {
    return axios.get(`/faqs/${id}`)
  },

  // Create new FAQ (Admin only)
  createFaq(faq: Omit<Faq, 'id' | 'createdAt'>): Promise<Faq> {
    return axios.post('/faqs', faq)
  },

  // Update existing FAQ (Admin only)
  updateFaq(id: number, faq: Omit<Faq, 'id' | 'createdAt'>): Promise<Faq> {
    return axios.put(`/faqs/${id}`, faq)
  },

  // Delete FAQ (Admin only)
  deleteFaq(id: number): Promise<void> {
    return axios.delete(`/faqs/${id}`)
  }
}
