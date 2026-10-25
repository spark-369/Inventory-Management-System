import { useCallback, useRef, useState, type ReactNode } from 'react'

export interface ConfirmOptions {
  title: string
  message: ReactNode
  confirmLabel?: string
  cancelLabel?: string
  tone?: 'danger' | 'primary'
}

interface ConfirmState extends ConfirmOptions {
  isOpen: boolean
}

const initialState: ConfirmState = {
  isOpen: false,
  title: '',
  message: '',
}

/**
 * Promise-based confirmation flow that replaces `window.confirm`.
 *
 * Usage:
 *   const { confirm, confirmState, handleConfirm, handleCancel } = useConfirm()
 *   if (await confirm({ title: 'Delete?', message: '…' })) { … }
 */
export function useConfirm() {
  const [state, setState] = useState<ConfirmState>(initialState)
  const resolverRef = useRef<((value: boolean) => void) | null>(null)

  const confirm = useCallback((options: ConfirmOptions) => {
    setState({ ...options, isOpen: true })
    return new Promise<boolean>((resolve) => {
      resolverRef.current = resolve
    })
  }, [])

  const settle = useCallback((result: boolean) => {
    resolverRef.current?.(result)
    resolverRef.current = null
    setState(initialState)
  }, [])

  const handleConfirm = useCallback(() => settle(true), [settle])
  const handleCancel = useCallback(() => settle(false), [settle])

  return {
    confirm,
    confirmState: state,
    handleConfirm,
    handleCancel,
  }
}
