import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { ProductDto } from '../types/api'

interface ProductsState {
  items: ProductDto[]
  loading: boolean
  error: string | null
}

const initialState: ProductsState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchProducts = createAsyncThunk<ProductDto[]>(
  'products/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<ProductDto[]>('/products')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createProduct = createAsyncThunk<ProductDto, ProductDto>(
  'products/create',
  async (product, { rejectWithValue }) => {
    try {
      return await api.post<ProductDto>('/products', product)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updateProduct = createAsyncThunk<ProductDto, { id: number; data: ProductDto }>(
  'products/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<ProductDto>(`/products/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deleteProduct = createAsyncThunk<number, number>(
  'products/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/products/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const productsSlice = createSlice({
  name: 'products',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchProducts.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchProducts.fulfilled, (state, action: PayloadAction<ProductDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchProducts.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createProduct.fulfilled, (state, action: PayloadAction<ProductDto>) => {
        state.items.push(action.payload)
      })
      .addCase(updateProduct.fulfilled, (state, action: PayloadAction<ProductDto>) => {
        const idx = state.items.findIndex((p) => p.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deleteProduct.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((p) => p.id !== action.payload)
      })
  },
})

export default productsSlice.reducer
