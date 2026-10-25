import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { SaleDto } from '../types/api'

interface SalesState {
  items: SaleDto[]
  loading: boolean
  error: string | null
}

const initialState: SalesState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchSales = createAsyncThunk<SaleDto[]>(
  'sales/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<SaleDto[]>('/sales')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createSale = createAsyncThunk<SaleDto, SaleDto>(
  'sales/create',
  async (sale, { rejectWithValue }) => {
    try {
      return await api.post<SaleDto>('/sales', sale)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updateSale = createAsyncThunk<SaleDto, { id: number; data: SaleDto }>(
  'sales/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<SaleDto>(`/sales/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const completeSale = createAsyncThunk<SaleDto, number>(
  'sales/complete',
  async (id, { rejectWithValue }) => {
    try {
      return await api.post<SaleDto>(`/sales/${id}/complete`, {})
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deleteSale = createAsyncThunk<number, number>(
  'sales/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/sales/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const salesSlice = createSlice({
  name: 'sales',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchSales.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchSales.fulfilled, (state, action: PayloadAction<SaleDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchSales.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createSale.fulfilled, (state, action: PayloadAction<SaleDto>) => {
        state.items.unshift(action.payload)
      })
      .addCase(updateSale.fulfilled, (state, action: PayloadAction<SaleDto>) => {
        const idx = state.items.findIndex((s) => s.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(completeSale.fulfilled, (state, action: PayloadAction<SaleDto>) => {
        const idx = state.items.findIndex((s) => s.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deleteSale.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((s) => s.id !== action.payload)
      })
  },
})

export default salesSlice.reducer
