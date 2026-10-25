import type { ReactNode } from 'react'
import { InboxIcon } from './icons'

interface EmptyStateProps {
  title: string
  description?: string
  icon?: ReactNode
  action?: ReactNode
}

/** Friendly placeholder shown when a collection has no rows. */
export function EmptyState({ title, description, icon, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-16 text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-ink-100 text-xl text-ink-400">
        {icon ?? <InboxIcon />}
      </div>
      <div className="space-y-1">
        <p className="text-sm font-semibold text-ink-800">{title}</p>
        {description && <p className="mx-auto max-w-sm text-sm text-ink-500">{description}</p>}
      </div>
      {action}
    </div>
  )
}

interface ErrorStateProps {
  message: string
  onRetry?: () => void
}

/** Error placeholder with an optional retry affordance. */
export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-16 text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-red-50 text-xl text-red-500">!</div>
      <div className="space-y-1">
        <p className="text-sm font-semibold text-ink-800">Could not load data</p>
        <p className="mx-auto max-w-md text-sm text-ink-500">{message}</p>
      </div>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="rounded-lg border border-ink-300 bg-white px-3 py-1.5 text-sm font-medium text-ink-700 hover:bg-ink-50"
        >
          Try again
        </button>
      )}
    </div>
  )
}

/** Skeleton row used while a table is loading. */
export function TableSkeleton({ rows = 6, columns = 5 }: { rows?: number; columns?: number }) {
  return (
    <div className="divide-y divide-ink-100" aria-hidden="true">
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div key={rowIndex} className="flex items-center gap-4 px-6 py-4">
          {Array.from({ length: columns }).map((__, colIndex) => (
            <div
              key={colIndex}
              className={`h-4 animate-pulse rounded bg-ink-100 ${colIndex === 0 ? 'w-16' : 'flex-1'}`}
            />
          ))}
        </div>
      ))}
    </div>
  )
}
