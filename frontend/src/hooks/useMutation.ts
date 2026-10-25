import { useCallback, useState } from 'react'
import toast from 'react-hot-toast'
import { useAppDispatch } from './redux'
import { getErrorMessage } from '../services/api'

interface RunOptions {
  /** Toast shown on success. Pass `null` to stay silent. */
  successMessage?: string | null
  /** Fallback toast shown on failure; the server message takes priority. */
  errorMessage?: string
  onSuccess?: () => void
  onError?: (message: string) => void
}

/**
 * Wraps an async Redux thunk dispatch so components get loading state,
 * automatic success/error toasts and a single error message — instead of
 * every page hand-rolling `dispatch(...).unwrap().then(...)`.
 */
export function useMutation() {
  const dispatch = useAppDispatch()
  const [pending, setPending] = useState(false)

  const run = useCallback(
    async <T,>(
      thunk: unknown,
      { successMessage, errorMessage, onSuccess, onError }: RunOptions = {},
    ): Promise<T | undefined> => {
      setPending(true)
      try {
        const action = dispatch(thunk as never) as unknown as { unwrap: () => Promise<T> }
        const result = await action.unwrap()
        if (successMessage) toast.success(successMessage)
        onSuccess?.()
        return result
      } catch (error) {
        const message = getErrorMessage(error) || errorMessage || 'Request failed.'
        toast.error(message)
        onError?.(message)
        return undefined
      } finally {
        setPending(false)
      }
    },
    [dispatch],
  )

  return { run, pending }
}
