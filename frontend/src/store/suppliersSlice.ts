import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { SupplierDto } from '../types/api'

interface SuppliersState {
  items: SupplierDto[]
  loading: boolean
  error: string | null
}

const initialState: SuppliersState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchSuppliers = createAsyncThunk<SupplierDto[]>(
  'suppliers/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<SupplierDto[]>('/suppliers')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createSupplier = createAsyncThunk<SupplierDto, SupplierDto>(
  'suppliers/create',
  async (supplier, { rejectWithValue }) => {
    try {
      return await api.post<SupplierDto>('/suppliers', supplier)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updateSupplier = createAsyncThunk<SupplierDto, { id: number; data: SupplierDto }>(
  'suppliers/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<SupplierDto>(`/suppliers/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deleteSupplier = createAsyncThunk<number, number>(
  'suppliers/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/suppliers/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const suppliersSlice = createSlice({
  name: 'suppliers',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchSuppliers.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchSuppliers.fulfilled, (state, action: PayloadAction<SupplierDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchSuppliers.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createSupplier.fulfilled, (state, action: PayloadAction<SupplierDto>) => {
        state.items.push(action.payload)
      })
      .addCase(updateSupplier.fulfilled, (state, action: PayloadAction<SupplierDto>) => {
        const idx = state.items.findIndex((s) => s.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deleteSupplier.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((s) => s.id !== action.payload)
      })
  },
})

export default suppliersSlice.reducer
