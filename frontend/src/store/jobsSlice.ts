import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import { api, getErrorMessage } from '../services/api'
import type {
  JobDispatchResult,
  JobMessageLogDto,
  JobMessageTypeOption,
  JobScheduleDto,
  JobStatsDto,
} from '../types/api'

interface JobsState {
  stats: JobStatsDto | null
  schedules: JobScheduleDto[]
  messages: JobMessageLogDto[]
  types: JobMessageTypeOption[]
  loading: boolean
  error: string | null
}

/** Everything the console needs in a single round trip. */
interface JobOverviewPayload {
  stats: JobStatsDto
  schedules: JobScheduleDto[]
  messages: JobMessageLogDto[]
  types: JobMessageTypeOption[]
}

const initialState: JobsState = {
  stats: null,
  schedules: [],
  messages: [],
  types: [],
  loading: false,
  error: null,
}

/** Loads broker counters, the declared schedules and recent message history. */
export const fetchJobOverview = createAsyncThunk<JobOverviewPayload, number | undefined>(
  'jobs/fetchOverview',
  async (limit = 50, { rejectWithValue }) => {
    try {
      const [stats, schedules, messages, types] = await Promise.all([
        api.get<JobStatsDto>('/jobs/stats'),
        api.get<JobScheduleDto[]>('/jobs/schedules'),
        api.get<JobMessageLogDto[]>('/jobs/messages', { limit }),
        api.get<JobMessageTypeOption[]>('/jobs/types'),
      ])
      return { stats, schedules, messages, types }
    } catch (error) {
      return rejectWithValue(getErrorMessage(error))
    }
  }
)

/** Publishes a message on demand through the broker. */
export const dispatchJobMessage = createAsyncThunk<
  JobDispatchResult,
  { type: string; productId?: number }
>('jobs/dispatch', async ({ type, productId }, { rejectWithValue }) => {
  try {
    return await api.post<JobDispatchResult>('/jobs/dispatch', { type, productId })
  } catch (error) {
    return rejectWithValue(getErrorMessage(error))
  }
})

const jobsSlice = createSlice({
  name: 'jobs',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchJobOverview.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchJobOverview.fulfilled, (state, action: PayloadAction<JobOverviewPayload>) => {
        state.loading = false
        state.stats = action.payload.stats
        state.schedules = action.payload.schedules
        state.messages = action.payload.messages
        state.types = action.payload.types
      })
      .addCase(fetchJobOverview.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(dispatchJobMessage.fulfilled, (state) => {
        state.error = null
      })
  },
})

export default jobsSlice.reducer
