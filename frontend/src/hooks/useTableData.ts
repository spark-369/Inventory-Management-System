import { useEffect, useMemo, useState } from 'react'
import type { SortState } from '../components/ui/DataTable'

interface UseTableDataOptions<T> {
  rows: T[]
  /** Fields searched by the free-text query. */
  searchable?: (row: T) => Array<string | number | null | undefined>
  /** Field extractors keyed by column key, enabling per-column sorting. */
  sortValues?: Record<string, (row: T) => string | number | null | undefined>
  initialPageSize?: number
}

/**
 * Client-side search, sort and pagination for list pages.
 * Resets to page 1 whenever the query, sort or page size changes.
 */
export function useTableData<T>({
  rows,
  searchable,
  sortValues,
  initialPageSize = 10,
}: UseTableDataOptions<T>) {
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<SortState | undefined>()
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(initialPageSize)

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase()
    if (!term || !searchable) return rows
    return rows.filter((row) =>
      searchable(row).some((value) => String(value ?? '').toLowerCase().includes(term)),
    )
  }, [rows, query, searchable])

  const sorted = useMemo(() => {
    if (!sort || !sortValues?.[sort.key]) return filtered
    const extract = sortValues[sort.key]
    const factor = sort.direction === 'asc' ? 1 : -1
    return [...filtered].sort((a, b) => {
      const left = extract(a)
      const right = extract(b)
      if (left == null && right == null) return 0
      if (left == null) return 1
      if (right == null) return -1
      if (typeof left === 'number' && typeof right === 'number') return (left - right) * factor
      return String(left).localeCompare(String(right), undefined, { numeric: true, sensitivity: 'base' }) * factor
    })
  }, [filtered, sort, sortValues])

  const total = sorted.length
  const pageCount = Math.max(1, Math.ceil(total / pageSize))

  // Keep the current page in range when filters shrink the result set.
  useEffect(() => {
    if (page > pageCount) setPage(pageCount)
  }, [page, pageCount])

  useEffect(() => {
    setPage(1)
  }, [query, sort, pageSize])

  const paged = useMemo(
    () => sorted.slice((page - 1) * pageSize, page * pageSize),
    [sorted, page, pageSize],
  )

  return {
    query,
    setQuery,
    sort,
    setSort,
    page,
    setPage,
    pageSize,
    setPageSize,
    /** Rows after search + sort but before pagination. */
    filtered: sorted,
    /** Rows for the current page only. */
    rows: paged,
    total,
    pageCount,
    isFiltered: query.trim().length > 0,
  }
}
