import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api } from '../services/api'
import type { CustomerDto } from '../types/api'

interface CustomersState {
  items: CustomerDto[]
  loading: boolean
  error: string | null
}

const initialState: CustomersState = {
  items: [],
  loading: false,
  error: null,
}

export const fetchCustomers = createAsyncThunk<CustomerDto[]>(
  'customers/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await api.get<CustomerDto[]>('/customers')
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const createCustomer = createAsyncThunk<CustomerDto, CustomerDto>(
  'customers/create',
  async (customer, { rejectWithValue }) => {
    try {
      return await api.post<CustomerDto>('/customers', customer)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const updateCustomer = createAsyncThunk<CustomerDto, { id: number; data: CustomerDto }>(
  'customers/update',
  async ({ id, data }, { rejectWithValue }) => {
    try {
      return await api.put<CustomerDto>(`/customers/${id}`, data)
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

export const deleteCustomer = createAsyncThunk<number, number>(
  'customers/delete',
  async (id, { rejectWithValue }) => {
    try {
      await api.delete(`/customers/${id}`)
      return id
    } catch (error) {
      return rejectWithValue((error as Error).message)
    }
  }
)

const customersSlice = createSlice({
  name: 'customers',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchCustomers.pending, (state) => { state.loading = true; state.error = null })
      .addCase(fetchCustomers.fulfilled, (state, action: PayloadAction<CustomerDto[]>) => {
        state.loading = false
        state.items = action.payload
      })
      .addCase(fetchCustomers.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createCustomer.fulfilled, (state, action: PayloadAction<CustomerDto>) => {
        state.items.push(action.payload)
      })
      .addCase(updateCustomer.fulfilled, (state, action: PayloadAction<CustomerDto>) => {
        const idx = state.items.findIndex((c) => c.id === action.payload.id)
        if (idx !== -1) state.items[idx] = action.payload
      })
      .addCase(deleteCustomer.fulfilled, (state, action: PayloadAction<number>) => {
        state.items = state.items.filter((c) => c.id !== action.payload)
      })
  },
})

export default customersSlice.reducer
