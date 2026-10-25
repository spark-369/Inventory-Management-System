import { useEffect, useMemo, useState } from 'react'
import { useAppDispatch, useAppSelector, useMutation } from '../hooks'
import { fetchAlerts, generateAlerts, resolveAlert } from '../store/alertsSlice'
import {
  Badge,
  Button,
  DataTable,
  EmptyState,
  ErrorState,
  PageHeader,
  Pagination,
  SearchBar,
  SelectField,
  StatusBadge,
  TableSkeleton,
  Toolbar,
  type Column,
} from '../components/ui'
import { AlertTriangleIcon, BellIcon, CheckIcon, RefreshIcon } from '../components/ui/icons'
import { daysUntil, formatDate, formatDateTime, humanize } from '../utils/format'
import type { AlertDto } from '../types/api'

const PAGE_SIZE = 10

export default function AlertsPage() {
  const dispatch = useAppDispatch()
  const { items: alerts, loading, error } = useAppSelector((state) => state.alerts)
  const { run, pending } = useMutation()

  const [includeResolved, setIncludeResolved] = useState(false)
  const [query, setQuery] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [page, setPage] = useState(1)

  useEffect(() => {
    dispatch(fetchAlerts(includeResolved))
  }, [dispatch, includeResolved])

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase()
    return alerts.filter((alert) => {
      const matchesType = !typeFilter || alert.type === typeFilter
      const matchesTerm =
        !term ||
        [alert.message, alert.productName, alert.batchNumber]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(term))
      return matchesType && matchesTerm
    })
  }, [alerts, query, typeFilter])

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, pageCount)
  const paged = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  const unresolvedCount = alerts.filter((alert) => !alert.resolved).length
  const criticalCount = alerts.filter((alert) => alert.type === 'EXPIRY_ALERT').length

  const handleResolve = async (alert: AlertDto) => {
    if (!alert.id) return
    await run(resolveAlert(alert.id), { successMessage: 'Alert resolved.' })
  }

  const handleGenerate = async () => {
    await run(generateAlerts(), {
      successMessage: 'Alert scan queued — results appear shortly.',
      onSuccess: () => dispatch(fetchAlerts(includeResolved)),
    })
  }

  const columns: Column<AlertDto>[] = [
    {
      key: 'type',
      header: 'Type',
      render: (row) => (
        <Badge tone={row.type === 'LOW_STOCK' ? 'warning' : row.type === 'EXPIRY_ALERT' ? 'danger' : 'info'}>
          {humanize(row.type)}
        </Badge>
      ),
    },
    {
      key: 'message',
      header: 'Message',
      render: (row) => <span className="text-ink-700">{row.message ?? '—'}</span>,
    },
    {
      key: 'productName',
      header: 'Product',
      render: (row) => <span className="font-medium text-ink-900">{row.productName ?? '—'}</span>,
    },
    {
      key: 'batchNumber',
      header: 'Batch',
      render: (row) => <span className="text-ink-600">{row.batchNumber || '—'}</span>,
    },
    {
      key: 'expiryDate',
      header: 'Expiry',
      render: (row) => {
        const remaining = daysUntil(row.expiryDate)
        if (remaining === null) return <span className="text-ink-400">—</span>
        return (
          <span className={remaining < 0 ? 'font-medium text-red-600' : 'text-ink-600'}>
            {formatDate(row.expiryDate)}
          </span>
        )
      },
    },
    {
      key: 'createdAt',
      header: 'Raised',
      render: (row) => (
        <span className="whitespace-nowrap text-ink-500">{formatDateTime(row.createdAt)}</span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge value={row.resolved ? 'COMPLETED' : 'PENDING'} />,
    },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (row) =>
        row.resolved ? (
          <span className="text-xs text-ink-400">Resolved</span>
        ) : (
          <Button
            size="sm"
            variant="success"
            icon={<CheckIcon />}
            loading={pending}
            onClick={() => void handleResolve(row)}
          >
            Resolve
          </Button>
        ),
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="Alerts"
        description="Low-stock and expiry notifications generated automatically or on demand."
        actions={
          <Button icon={<RefreshIcon />} onClick={handleGenerate} loading={pending}>
            Run alert scan
          </Button>
        }
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="card flex items-center gap-4 p-4">
          <span className="flex size-10 items-center justify-center rounded-lg bg-amber-50 text-xl text-amber-600">
            <BellIcon />
          </span>
          <span>
            <span className="block text-xs text-ink-500">Unresolved</span>
            <span className="block text-lg font-semibold text-ink-900">{unresolvedCount}</span>
          </span>
        </div>
        <div className="card flex items-center gap-4 p-4">
          <span className="flex size-10 items-center justify-center rounded-lg bg-red-50 text-xl text-red-600">
            <AlertTriangleIcon />
          </span>
          <span>
            <span className="block text-xs text-ink-500">Expired batches</span>
            <span className="block text-lg font-semibold text-ink-900">{criticalCount}</span>
          </span>
        </div>
        <div className="card flex items-center gap-4 p-4">
          <span className="flex size-10 items-center justify-center rounded-lg bg-ink-100 text-xl text-ink-600">
            <BellIcon />
          </span>
          <span>
            <span className="block text-xs text-ink-500">Loaded</span>
            <span className="block text-lg font-semibold text-ink-900">{alerts.length}</span>
          </span>
        </div>
      </div>

      <Toolbar end={<span>{filtered.length} alerts</span>}>
        <SearchBar value={query} onChange={setQuery} placeholder="Search message or product…" />
        <div className="w-48">
          <SelectField value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
            <option value="">All types</option>
            <option value="LOW_STOCK">Low stock</option>
            <option value="EXPIRY_WARNING">Expiry warning</option>
            <option value="EXPIRY_ALERT">Expired</option>
          </SelectField>
        </div>
        <label className="flex items-center gap-2 text-sm text-ink-600">
          <input
            type="checkbox"
            checked={includeResolved}
            onChange={(event) => setIncludeResolved(event.target.checked)}
            className="size-4 rounded border-ink-300 text-brand-600 focus:ring-brand-500"
          />
          Show resolved
        </label>
      </Toolbar>

      <DataTable
        columns={columns}
        rows={paged}
        rowKey={(row) => row.id!}
        loading={loading}
        skeleton={<TableSkeleton columns={columns.length} />}
        caption="Inventory alerts"
        emptyState={
          error ? (
            <ErrorState message={error} onRetry={() => dispatch(fetchAlerts(includeResolved))} />
          ) : filtered.length === 0 && (query || typeFilter) ? (
            <EmptyState
              title="No matches"
              description="No alert matches the current filters."
              icon={<BellIcon />}
            />
          ) : (
            <EmptyState
              title="No open alerts"
              description="Stock levels and expiry dates all look healthy."
              icon={<BellIcon />}
              action={
                <Button size="sm" icon={<RefreshIcon />} onClick={handleGenerate} loading={pending}>
                  Run alert scan
                </Button>
              }
            />
          )
        }
        footer={
          <Pagination
            page={currentPage}
            pageSize={PAGE_SIZE}
            total={filtered.length}
            onPageChange={setPage}
          />
        }
      />
    </div>
  )
}
