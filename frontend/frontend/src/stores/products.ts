import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import api from '@/api/auth'

export interface Product {
  id: number
  name: string
  price: number
  description: string
  stock: number
  image: string
  status: 'ACTIVE' | 'INACTIVE'
  category: string
  createdAt?: string
  stockThreshold?: number
}

export interface ProductFormData {
  name: string
  price: number
  description: string
  stock: number
  status: 'ACTIVE' | 'INACTIVE'
  category: string
  stockThreshold?: number
}

export interface StockAdjustment {
  type: 'increase' | 'decrease'
  quantity: number
  reason: string
}

export interface ProductFilter {
  search: string
  category: string
  priceMin: number | null
  priceMax: number | null
  showInactive: boolean
}

export interface PaginationInfo {
  currentPage: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export const useProductsStore = defineStore('products', () => {
  const products = ref<Product[]>([])
  const loading = ref(false)
  const sortBy = ref<'name' | 'price' | 'stock' | null>(null)
  const sortOrder = ref<'asc' | 'desc'>('asc')
  const filter = ref<ProductFilter>({
    search: '',
    category: '',
    priceMin: null,
    priceMax: null,
    showInactive: false
  })
  const pagination = ref<PaginationInfo>({
    currentPage: 1,
    pageSize: 10,
    totalItems: 0,
    totalPages: 0
  })

  // Mock data for development
  const mockProducts: Product[] = [
    {
      id: 1,
      name: '測試商品1',
      price: 199.99,
      description: '這是測試用商品1的描述',
      stock: 10,
      image: '/images/product1.jpg',
      status: 'ACTIVE',
      category: '測試類別',
      createdAt: new Date('2024-01-01').toISOString(),
      stockThreshold: 5
    },
    {
      id: 2,
      name: '測試商品2',
      price: 299.99,
      description: '這是測試用商品2的描述',
      stock: 15,
      image: '/images/product2.jpg',
      status: 'ACTIVE',
      category: '測試類別',
      createdAt: new Date('2024-01-02').toISOString(),
      stockThreshold: 5
    },
    {
      id: 3,
      name: '測試商品 3',
      price: 99,
      description: '經濟實惠的選擇，性價比極高。',
      stock: 1,
      image: '/images/product3.jpg',
      status: 'ACTIVE',
      category: '食品'
    },
    {
      id: 4,
      name: '下架商品',
      price: 159,
      description: '這個商品已經下架。',
      stock: 0,
      image: '/images/product4.jpg',
      status: 'INACTIVE',
      category: '電子產品'
    },
    {
      id: 5,
      name: '高價商品',
      price: 999,
      description: '高端商品，品質卓越。',
      stock: 3,
      image: '/images/product5.jpg',
      status: 'ACTIVE',
      category: '奢侈品'
    },
    {
      id: 6,
      name: '測試商品 6',
      price: 249,
      description: '第六個測試商品，品質優良。',
      stock: 8,
      image: '/images/product6.jpg',
      status: 'ACTIVE',
      category: '電子產品'
    },
    {
      id: 7,
      name: '測試商品 7',
      price: 149,
      description: '第七個測試商品，價格實惠。',
      stock: 15,
      image: '/images/product7.jpg',
      status: 'ACTIVE',
      category: '食品'
    },
    {
      id: 8,
      name: '測試商品 8',
      price: 399,
      description: '第八個測試商品，功能強大。',
      stock: 6,
      image: '/images/product8.jpg',
      status: 'ACTIVE',
      category: '電子產品'
    },
    {
      id: 9,
      name: '測試商品 9',
      price: 79,
      description: '第九個測試商品，經濟實惠。',
      stock: 20,
      image: '/images/product9.jpg',
      status: 'ACTIVE',
      category: '食品'
    },
    {
      id: 10,
      name: '測試商品 10',
      price: 599,
      description: '第十個測試商品，高品質選擇。',
      stock: 4,
      image: '/images/product10.jpg',
      status: 'ACTIVE',
      category: '奢侈品'
    },
    {
      id: 11,
      name: '測試商品 11',
      price: 189,
      description: '第十一個測試商品。',
      stock: 12,
      image: '/images/product11.jpg',
      status: 'ACTIVE',
      category: '電子產品'
    },
    {
      id: 12,
      name: '測試商品 12',
      price: 129,
      description: '第十二個測試商品。',
      stock: 7,
      image: '/images/product12.jpg',
      status: 'ACTIVE',
      category: '食品'
    }
  ]

  // Computed properties
  const filteredProducts = computed(() => {
    let result = [...products.value]

    // Filter by status (inactive products only visible to admin if enabled)
    if (!filter.value.showInactive) {
      result = result.filter(p => p.status === 'ACTIVE')
    }

    // Search filter
    if (filter.value.search) {
      const searchLower = filter.value.search.toLowerCase()
      result = result.filter(p =>
        p.name.toLowerCase().includes(searchLower) ||
        p.description.toLowerCase().includes(searchLower)
      )
    }

    // Category filter
    if (filter.value.category) {
      result = result.filter(p => p.category === filter.value.category)
    }

    // Price range filter
    if (filter.value.priceMin !== null) {
      result = result.filter(p => p.price >= filter.value.priceMin!)
    }
    if (filter.value.priceMax !== null) {
      result = result.filter(p => p.price <= filter.value.priceMax!)
    }

    // Apply sorting - default to newest first (by createdAt)
    result.sort((a, b) => {
      let comparison = 0

      switch (sortBy.value) {
        case 'name':
          comparison = a.name.localeCompare(b.name)
          break
        case 'price':
          comparison = a.price - b.price
          break
        case 'stock':
          comparison = a.stock - b.stock
          break
        default:
          // Sort by creation date (newest first)
          const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
          const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
          comparison = dateB - dateA
      }

      return sortOrder.value === 'desc' ? -comparison : comparison
    })

    return result
  })

  const paginatedProducts = computed(() => {
    const start = (pagination.value.currentPage - 1) * pagination.value.pageSize
    const end = start + pagination.value.pageSize
    return filteredProducts.value.slice(start, end)
  })

  const categories = computed(() => {
    const categorySet = new Set(mockProducts.map(p => p.category))
    return Array.from(categorySet)
  })

  // Actions
  const loadProducts = async () => {
    loading.value = true
    try {
      // Call real API
      const response = await api.get<any>('/api/product')
      // API returns paginated format: { content: [...], totalElements: n }
      products.value = response.data.content || response.data

      // Force update pagination after products are loaded
      setTimeout(() => {
        updatePagination()
      }, 0)
    } catch (error) {
      console.error('Failed to load products:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  const updatePagination = () => {
    pagination.value.totalItems = filteredProducts.value.length
    pagination.value.totalPages = Math.ceil(pagination.value.totalItems / pagination.value.pageSize)

    // Reset to first page if current page is out of range
    if (pagination.value.currentPage > pagination.value.totalPages && pagination.value.totalPages > 0) {
      pagination.value.currentPage = 1
    }
  }

  const setFilter = (newFilter: Partial<ProductFilter>) => {
    filter.value = { ...filter.value, ...newFilter }
    pagination.value.currentPage = 1 // Reset to first page when filtering
    updatePagination()
  }

  const setPagination = (page: number, pageSize?: number) => {
    pagination.value.currentPage = page
    if (pageSize) {
      pagination.value.pageSize = pageSize
    }
    updatePagination()
  }

  // Watch for filter changes to update pagination automatically
  const initializeWatchers = () => {
    // This will be called after the store is set up
  }

  const sortProducts = (sortByField: 'name' | 'price' | 'stock', order: 'asc' | 'desc' = 'asc') => {
    sortBy.value = sortByField
    sortOrder.value = order
    updatePagination()
  }

  const toggleProductStatus = async (productId: number) => {
    // Update in mockProducts (the source of truth)
    const product = mockProducts.find(p => p.id === productId)
    if (product) {
      product.status = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 300))
      // Update the reactive products array
      products.value = [...mockProducts]
      updatePagination()
    }
  }

  const getProductById = (id: number) => {
    // 從 products 查找（API 載入的真實資料）
    return products.value.find(p => p.id === id)
  }

  // CRUD Operations
  const createProduct = async (productData: ProductFormData) => {
    loading.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 300))

      const newProduct: Product = {
        id: Math.max(...mockProducts.map(p => p.id)) + 1,
        name: productData.name,
        price: productData.price,
        description: productData.description,
        stock: productData.stock,
        image: '/images/placeholder.jpg',
        status: productData.status,
        category: productData.category,
        createdAt: new Date().toISOString(),
        stockThreshold: productData.stockThreshold || 5
      }

      mockProducts.push(newProduct)
      products.value = [...mockProducts]
      updatePagination()

      return { success: true, message: '商品已成功創建', product: newProduct }
    } catch (error) {
      return { success: false, message: '創建商品失敗' }
    } finally {
      loading.value = false
    }
  }

  const updateProduct = async (id: number, productData: Partial<ProductFormData>) => {
    loading.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 300))

      const productIndex = mockProducts.findIndex(p => p.id === id)
      if (productIndex === -1) {
        return { success: false, message: '商品不存在' }
      }

      const product = mockProducts[productIndex]
      mockProducts[productIndex] = {
        ...product,
        ...productData,
        price: productData.price !== undefined ? productData.price : product.price,
        stock: productData.stock !== undefined ? productData.stock : product.stock
      }

      products.value = [...mockProducts]
      updatePagination()

      return { success: true, message: '商品已成功更新' }
    } catch (error) {
      return { success: false, message: '更新商品失敗' }
    } finally {
      loading.value = false
    }
  }

  const deleteProduct = async (id: number) => {
    loading.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 300))

      const index = mockProducts.findIndex(p => p.id === id)
      if (index > -1) {
        mockProducts.splice(index, 1)
        products.value = [...mockProducts]
        updatePagination()
        return { success: true, message: '商品已刪除' }
      }
      return { success: false, message: '商品不存在' }
    } catch (error) {
      return { success: false, message: '刪除商品失敗' }
    } finally {
      loading.value = false
    }
  }

  const adjustStock = async (id: number, adjustment: StockAdjustment) => {
    loading.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 300))

      const product = mockProducts.find(p => p.id === id)
      if (!product) {
        return { success: false, message: '商品不存在' }
      }

      const adjustmentAmount = adjustment.type === 'increase' ? adjustment.quantity : -adjustment.quantity
      const newStock = product.stock + adjustmentAmount

      if (newStock < 0) {
        return { success: false, message: '庫存不能為負數' }
      }

      product.stock = newStock
      products.value = [...mockProducts]

      return { success: true, message: '庫存已調整', newStock }
    } catch (error) {
      return { success: false, message: '調整庫存失敗' }
    } finally {
      loading.value = false
    }
  }

  const batchUpdateStatus = async (ids: number[], status: 'ACTIVE' | 'INACTIVE') => {
    loading.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 300))

      ids.forEach(id => {
        const product = mockProducts.find(p => p.id === id)
        if (product) {
          product.status = status
        }
      })

      products.value = [...mockProducts]
      updatePagination()

      return { success: true, message: '已成功處理' }
    } catch (error) {
      return { success: false, message: '批量操作失敗' }
    } finally {
      loading.value = false
    }
  }

  const exportProducts = async (format: 'CSV' | 'JSON') => {
    loading.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 500))

      if (format === 'CSV') {
        const csvContent = [
          ['ID', '名稱', '價格', '庫存', '狀態', '類別', '建立時間'].join(','),
          ...filteredProducts.value.map(p =>
            [p.id, p.name, p.price, p.stock, p.status, p.category, p.createdAt].join(',')
          )
        ].join('\n')

        const blob = new Blob([csvContent], { type: 'text/csv' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `products_${new Date().toISOString().split('T')[0]}.csv`
        a.click()
        window.URL.revokeObjectURL(url)
      }

      return { success: true, message: '匯出成功' }
    } catch (error) {
      return { success: false, message: '匯出失敗' }
    } finally {
      loading.value = false
    }
  }

  // Low stock products
  const lowStockProducts = computed(() => {
    return mockProducts.filter(p => p.stock <= (p.stockThreshold || 5) && p.status === 'ACTIVE')
  })

  return {
    products,
    loading,
    filter,
    pagination,
    sortBy,
    sortOrder,
    filteredProducts,
    paginatedProducts,
    categories,
    lowStockProducts,
    loadProducts,
    setFilter,
    setPagination,
    sortProducts,
    toggleProductStatus,
    getProductById,
    updatePagination,
    createProduct,
    updateProduct,
    deleteProduct,
    adjustStock,
    batchUpdateStatus,
    exportProducts
  }
})