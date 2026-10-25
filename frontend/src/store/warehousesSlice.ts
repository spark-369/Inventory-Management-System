import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { WarehouseDto } from '../types/api'

interface WarehousesState {
  items: WarehouseDto[]
  loading: boolean
  error: string | null
}

const initialState: WarehousesState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchWarehouses = createAsyncThunk<WarehouseDto[]>(
  'warehouses/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<WarehouseDto[]>('/warehouses')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createWarehouse = createAsyncThunk<WarehouseDto, WarehouseDto>(
  'warehouses/create',
  async (warehouse, { rejectWithValue }) => {
    try {
      return await api.post<WarehouseDto>('/warehouses', warehouse)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updateWarehouse = createAsyncThunk<WarehouseDto, { id: number; data: WarehouseDto }>(
  'warehouses/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<WarehouseDto>(`/warehouses/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deleteWarehouse = createAsyncThunk<number, number>(
  'warehouses/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/warehouses/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const warehousesSlice = createSlice({
  name: 'warehouses',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchWarehouses.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchWarehouses.fulfilled, (state, action: PayloadAction<WarehouseDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchWarehouses.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createWarehouse.fulfilled, (state, action: PayloadAction<WarehouseDto>) => {
        state.items.push(action.payload)
      })
      .addCase(updateWarehouse.fulfilled, (state, action: PayloadAction<WarehouseDto>) => {
        const idx = state.items.findIndex((w) => w.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deleteWarehouse.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((w) => w.id !== action.payload)
      })
  },
})

export default warehousesSlice.reducer
