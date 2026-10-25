import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { StockBatchDto } from '../types/api'

interface StockBatchesState {
  items: StockBatchDto[]
  loading: boolean
  error: string | null
}

const initialState: StockBatchesState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchStockBatches = createAsyncThunk<StockBatchDto[]>(
  'stockBatches/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<StockBatchDto[]>('/stock-batches')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createStockBatch = createAsyncThunk<StockBatchDto, StockBatchDto>(
  'stockBatches/create',
  async (batch, { rejectWithValue }) => {
    try {
      return await api.post<StockBatchDto>('/stock-batches', batch)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updateStockBatch = createAsyncThunk<StockBatchDto, { id: number; data: StockBatchDto }>(
  'stockBatches/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<StockBatchDto>(`/stock-batches/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deleteStockBatch = createAsyncThunk<number, number>(
  'stockBatches/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/stock-batches/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const stockBatchesSlice = createSlice({
  name: 'stockBatches',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchStockBatches.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchStockBatches.fulfilled, (state, action: PayloadAction<StockBatchDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchStockBatches.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createStockBatch.fulfilled, (state, action: PayloadAction<StockBatchDto>) => {
        state.items.push(action.payload)
      })
      .addCase(updateStockBatch.fulfilled, (state, action: PayloadAction<StockBatchDto>) => {
        const idx = state.items.findIndex((b) => b.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deleteStockBatch.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((b) => b.id !== action.payload)
      })
  },
})

export default stockBatchesSlice.reducer
