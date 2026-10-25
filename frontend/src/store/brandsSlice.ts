import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { BrandDto } from '../types/api'

interface BrandsState {
  items: BrandDto[]
  loading: boolean
  error: string | null
}

const initialState: BrandsState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchBrands = createAsyncThunk<BrandDto[]>(
  'brands/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<BrandDto[]>('/brands')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createBrand = createAsyncThunk<BrandDto, BrandDto>(
  'brands/create',
  async (brand, { rejectWithValue }) => {
    try {
      return await api.post<BrandDto>('/brands', brand)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updateBrand = createAsyncThunk<BrandDto, { id: number; data: BrandDto }>(
  'brands/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<BrandDto>(`/brands/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deleteBrand = createAsyncThunk<number, number>(
  'brands/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/brands/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const brandsSlice = createSlice({
  name: 'brands',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchBrands.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchBrands.fulfilled, (state, action: PayloadAction<BrandDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchBrands.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createBrand.fulfilled, (state, action: PayloadAction<BrandDto>) => {
        state.items.push(action.payload)
      })
      .addCase(updateBrand.fulfilled, (state, action: PayloadAction<BrandDto>) => {
        const idx = state.items.findIndex((b) => b.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deleteBrand.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((b) => b.id !== action.payload)
      })
  },
})

export default brandsSlice.reducer
