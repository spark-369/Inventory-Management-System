import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api, getErrorMessage } from '../services/api'
import type { AlertDto } from '../types/api'

interface AlertsState {
  items: AlertDto[]
  loading: boolean
  error: string | null
}

const initialState: AlertsState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchAlerts = createAsyncThunk<AlertDto[], boolean | undefined>(
  'alerts/fetchAll',
  async (includeResolved, { rejectWithValue }) => {
    try {
      return await api.get<AlertDto[]>(includeResolved ? '/alerts' : '/alerts/unresolved')
    } catch (error) {
      return rejectWithValue(getErrorMessage(error))
    }
  }
)

export const resolveAlert = createAsyncThunk<AlertDto, number>(
  'alerts/resolve',
  async (id, { rejectWithValue }) => {
    try {
      return await api.post<AlertDto>(`/alerts/${id}/resolve`)
    } catch (error) {
      return rejectWithValue(getErrorMessage(error))
    }
  }
)

export const generateAlerts = createAsyncThunk<void>(
  'alerts/generate',
  async (_, { rejectWithValue }) => {
    try {
      await api.post<void>('/alerts/generate')
    } catch (error) {
      return rejectWithValue(getErrorMessage(error))
    }
  }
)

const alertsSlice = createSlice({
  name: 'alerts',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchAlerts.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchAlerts.fulfilled, (state, action: PayloadAction<AlertDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchAlerts.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(resolveAlert.fulfilled, (state, action: PayloadAction<AlertDto>) => {
        const idx = state.items.findIndex((a) => a.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(generateAlerts.fulfilled, (state) => {
        state.error = null
      })
  },
})

export default alertsSlice.reducer
