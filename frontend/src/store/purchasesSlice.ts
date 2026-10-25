import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { PurchaseDto } from '../types/api'

interface PurchasesState {
  items: PurchaseDto[]
  loading: boolean
  error: string | null
}

const initialState: PurchasesState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchPurchases = createAsyncThunk<PurchaseDto[]>(
  'purchases/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<PurchaseDto[]>('/purchases')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createPurchase = createAsyncThunk<PurchaseDto, PurchaseDto>(
  'purchases/create',
  async (purchase, { rejectWithValue }) => {
    try {
      return await api.post<PurchaseDto>('/purchases', purchase)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updatePurchase = createAsyncThunk<PurchaseDto, { id: number; data: PurchaseDto }>(
  'purchases/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<PurchaseDto>(`/purchases/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const receivePurchase = createAsyncThunk<PurchaseDto, number>(
  'purchases/receive',
  async (id, { rejectWithValue }) => {
    try {
      return await api.post<PurchaseDto>(`/purchases/${id}/receive`, {})
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deletePurchase = createAsyncThunk<number, number>(
  'purchases/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/purchases/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const purchasesSlice = createSlice({
  name: 'purchases',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchPurchases.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchPurchases.fulfilled, (state, action: PayloadAction<PurchaseDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchPurchases.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createPurchase.fulfilled, (state, action: PayloadAction<PurchaseDto>) => {
        state.items.unshift(action.payload)
      })
      .addCase(updatePurchase.fulfilled, (state, action: PayloadAction<PurchaseDto>) => {
        const idx = state.items.findIndex((p) => p.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(receivePurchase.fulfilled, (state, action: PayloadAction<PurchaseDto>) => {
        const idx = state.items.findIndex((p) => p.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deletePurchase.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((p) => p.id !== action.payload)
      })
  },
})

export default purchasesSlice.reducer
