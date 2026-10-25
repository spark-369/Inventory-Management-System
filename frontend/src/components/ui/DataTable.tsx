import type { ReactNode } from 'react'

export interface Column<T> {
  key: string
  header: string
  /** Cell renderer; return any ReactNode. */
  render: (row: T) => ReactNode
  /** Enables client-side sorting for this column when a sort value is provided. */
  sortValue?: (row: T) => string | number | null | undefined
  className?: string
  headerClassName?: string
  align?: 'left' | 'right' | 'center'
}

export type SortDirection = 'asc' | 'desc'

export interface SortState {
  key: string
  direction: SortDirection
}

interface DataTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  sort?: SortState
  onSortChange?: (sort: SortState) => void
  /** Message shown when there are no rows to display. */
  emptyState?: ReactNode
  loading?: boolean
  skeleton?: ReactNode
  /** Rendered in the table footer (e.g. pagination). */
  footer?: ReactNode
  caption?: string
}

const alignClass = {
  left: 'text-left',
  right: 'text-right',
  center: 'text-center',
}

/**
 * Responsive, sortable table. On narrow screens the container scrolls
 * horizontally so columns never squash into unreadable slivers.
 */
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  sort,
  onSortChange,
  emptyState,
  loading = false,
  skeleton,
  footer,
  caption,
}: DataTableProps<T>) {
  const showEmpty = !loading && rows.length === 0

  return (
    <div className="card overflow-hidden">
      {caption && <p className="sr-only">{caption}</p>}

      {loading ? (
        skeleton ?? <div className="p-8 text-center text-sm text-ink-500">Loading…</div>
      ) : showEmpty ? (
        emptyState ?? <div className="p-8 text-center text-sm text-ink-500">No records found.</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="border-b border-ink-200 bg-ink-50/80">
                {columns.map((column) => {
                  const sortable = Boolean(column.sortValue && onSortChange)
                  const active = sort?.key === column.key
                  return (
                    <th
                      key={column.key}
                      scope="col"
                      aria-sort={active ? (sort?.direction === 'asc' ? 'ascending' : 'descending') : undefined}
                      className={`whitespace-nowrap px-4 py-3 text-xs font-semibold tracking-wide text-ink-500 uppercase ${
                        alignClass[column.align ?? 'left']
                      } ${column.headerClassName ?? ''}`}
                    >
                      {sortable ? (
                        <button
                          type="button"
                          onClick={() =>
                            onSortChange?.({
                              key: column.key,
                              direction: active && sort?.direction === 'asc' ? 'desc' : 'asc',
                            })
                          }
                          className={`inline-flex items-center gap-1 rounded transition hover:text-ink-800 ${
                            active ? 'text-brand-700' : ''
                          }`}
                        >
                          {column.header}
                          <span aria-hidden="true" className="text-[10px] leading-none">
                            {active ? (sort?.direction === 'asc' ? '▲' : '▼') : '⇅'}
                          </span>
                        </button>
                      ) : (
                        column.header
                      )}
                    </th>
                  )
                })}
              </tr>
            </thead>
            <tbody className="divide-y divide-ink-100">
              {rows.map((row) => (
                <tr key={rowKey(row)} className="transition-colors hover:bg-brand-50/40">
                  {columns.map((column) => (
                    <td
                      key={column.key}
                      className={`px-4 py-3 text-ink-700 ${alignClass[column.align ?? 'left']} ${column.className ?? ''}`}
                    >
                      {column.render(row)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {footer}
    </div>
  )
}
