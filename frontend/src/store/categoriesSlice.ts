import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { CategoryDto } from '../types/api'

interface CategoriesState {
  items: CategoryDto[]
  loading: boolean
  error: string | null
}

const initialState: CategoriesState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchCategories = createAsyncThunk<CategoryDto[]>(
  'categories/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<CategoryDto[]>('/categories')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createCategory = createAsyncThunk<CategoryDto, CategoryDto>(
  'categories/create',
  async (category, { rejectWithValue }) => {
    try {
      return await api.post<CategoryDto>('/categories', category)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updateCategory = createAsyncThunk<CategoryDto, { id: number; data: CategoryDto }>(
  'categories/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<CategoryDto>(`/categories/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deleteCategory = createAsyncThunk<number, number>(
  'categories/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/categories/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const categoriesSlice = createSlice({
  name: 'categories',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchCategories.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchCategories.fulfilled, (state, action: PayloadAction<CategoryDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchCategories.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createCategory.fulfilled, (state, action: PayloadAction<CategoryDto>) => {
        state.items.push(action.payload)
      })
      .addCase(updateCategory.fulfilled, (state, action: PayloadAction<CategoryDto>) => {
        const idx = state.items.findIndex((c) => c.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deleteCategory.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((c) => c.id !== action.payload)
      })
  },
})

export default categoriesSlice.reducer
