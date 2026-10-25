import { ChevronLeftIcon, ChevronRightIcon } from './icons'

interface PaginationProps {
  page: number
  pageSize: number
  total: number
  onPageChange: (page: number) => void
}

/** Compact paginator showing the current window and total record count. */
export function Pagination({ page, pageSize, total, onPageChange }: PaginationProps) {
  const pageCount = Math.max(1, Math.ceil(total / pageSize))
  if (total === 0) return null

  const from = (page - 1) * pageSize + 1
  const to = Math.min(page * pageSize, total)

  return (
    <div className="flex flex-col items-center justify-between gap-3 border-t border-ink-200 px-4 py-3 sm:flex-row sm:px-6">
      <p className="text-sm text-ink-500">
        Showing <span className="font-medium text-ink-700">{from}</span>–
        <span className="font-medium text-ink-700">{to}</span> of{' '}
        <span className="font-medium text-ink-700">{total}</span>
      </p>
      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 1}
          aria-label="Previous page"
          className="inline-flex size-8 items-center justify-center rounded-lg border border-ink-300 text-ink-600 transition hover:bg-ink-50 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <ChevronLeftIcon />
        </button>
        <span className="px-2 text-sm text-ink-600">
          Page {page} of {pageCount}
        </span>
        <button
          type="button"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= pageCount}
          aria-label="Next page"
          className="inline-flex size-8 items-center justify-center rounded-lg border border-ink-300 text-ink-600 transition hover:bg-ink-50 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <ChevronRightIcon />
        </button>
      </div>
    </div>
  )
}
