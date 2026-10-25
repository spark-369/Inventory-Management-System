import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { StockMovementDto } from '../types/api'

interface StockMovementsState {
  items: StockMovementDto[]
  loading: boolean
  error: string | null
}

const initialState: StockMovementsState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchStockMovements = createAsyncThunk<StockMovementDto[]>(
  'stockMovements/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<StockMovementDto[]>('/stock-movements')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createStockMovement = createAsyncThunk<StockMovementDto, StockMovementDto>(
  'stockMovements/create',
  async (movement, { rejectWithValue }) => {
    try {
      return await api.post<StockMovementDto>('/stock-movements', movement)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const stockMovementsSlice = createSlice({
  name: 'stockMovements',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchStockMovements.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchStockMovements.fulfilled, (state, action: PayloadAction<StockMovementDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchStockMovements.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createStockMovement.fulfilled, (state, action: PayloadAction<StockMovementDto>) => {
        state.items.unshift(action.payload)
      })
  },
})

export default stockMovementsSlice.reducer
