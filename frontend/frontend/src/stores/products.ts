import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export interface Product {
  id: number
  name: string
  price: number
  description: string
  stock: number
  image: string
  status: 'ACTIVE' | 'INACTIVE'
  category: string
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
      name: '測試商品 1',
      price: 199,
      description: '這是一個優質的測試商品，具有優良的品質保證。',
      stock: 10,
      image: '/images/product1.jpg',
      status: 'ACTIVE',
      category: '電子產品'
    },
    {
      id: 2,
      name: '測試商品 2',
      price: 299,
      description: '另一個精彩的測試商品，功能豐富實用。',
      stock: 5,
      image: '/images/product2.jpg',
      status: 'ACTIVE',
      category: '電子產品'
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
    }
  ]

  // Computed properties
  const filteredProducts = computed(() => {
    let result = [...mockProducts]

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
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 300))
      products.value = mockProducts
      updatePagination()
    } finally {
      loading.value = false
    }
  }

  const updatePagination = () => {
    pagination.value.totalItems = filteredProducts.value.length
    pagination.value.totalPages = Math.ceil(pagination.value.totalItems / pagination.value.pageSize)

    // Reset to first page if current page is out of range
    if (pagination.value.currentPage > pagination.value.totalPages) {
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

  const sortProducts = (sortBy: 'name' | 'price' | 'stock', order: 'asc' | 'desc' = 'asc') => {
    products.value.sort((a, b) => {
      let comparison = 0

      switch (sortBy) {
        case 'name':
          comparison = a.name.localeCompare(b.name)
          break
        case 'price':
          comparison = a.price - b.price
          break
        case 'stock':
          comparison = a.stock - b.stock
          break
      }

      return order === 'desc' ? -comparison : comparison
    })
  }

  const toggleProductStatus = async (productId: number) => {
    const product = products.value.find(p => p.id === productId)
    if (product) {
      product.status = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 300))
    }
  }

  const getProductById = (id: number) => {
    return products.value.find(p => p.id === id)
  }

  return {
    products,
    loading,
    filter,
    pagination,
    filteredProducts,
    paginatedProducts,
    categories,
    loadProducts,
    setFilter,
    setPagination,
    sortProducts,
    toggleProductStatus,
    getProductById,
    updatePagination
  }
})