import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { ReturnDto } from '../types/api'

interface ReturnsState {
  items: ReturnDto[]
  loading: boolean
  error: string | null
}

const initialState: ReturnsState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchReturns = createAsyncThunk<ReturnDto[]>(
  'returns/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<ReturnDto[]>('/returns')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createReturn = createAsyncThunk<ReturnDto, ReturnDto>(
  'returns/create',
  async (returnItem, { rejectWithValue }) => {
    try {
      return await api.post<ReturnDto>('/returns', returnItem)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updateReturn = createAsyncThunk<ReturnDto, { id: number; data: ReturnDto }>(
  'returns/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<ReturnDto>(`/returns/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const approveReturn = createAsyncThunk<ReturnDto, number>(
  'returns/approve',
  async (id, { rejectWithValue }) => {
    try {
      return await api.post<ReturnDto>(`/returns/${id}/approve`, {})
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deleteReturn = createAsyncThunk<number, number>(
  'returns/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/returns/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const returnsSlice = createSlice({
  name: 'returns',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchReturns.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchReturns.fulfilled, (state, action: PayloadAction<ReturnDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchReturns.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createReturn.fulfilled, (state, action: PayloadAction<ReturnDto>) => {
        state.items.unshift(action.payload)
      })
      .addCase(updateReturn.fulfilled, (state, action: PayloadAction<ReturnDto>) => {
        const idx = state.items.findIndex((r) => r.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(approveReturn.fulfilled, (state, action: PayloadAction<ReturnDto>) => {
        const idx = state.items.findIndex((r) => r.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deleteReturn.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((r) => r.id !== action.payload)
      })
  },
})

export default returnsSlice.reducer
