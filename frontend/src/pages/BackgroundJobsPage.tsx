import { useCallback, useEffect, useMemo, useState } from 'react'
import { useAppDispatch, useAppSelector, useMutation } from '../hooks'
import { dispatchJobMessage, fetchJobOverview } from '../store/jobsSlice'
import {
  Button,
  DataTable,
  EmptyState,
  ErrorState,
  PageHeader,
  Pagination,
  SearchBar,
  SelectField,
  StatCard,
  StatusBadge,
  TableSkeleton,
  Toolbar,
  type Column,
} from '../components/ui'
import {
  ActivityIcon,
  AlertTriangleIcon,
  ClockIcon,
  CpuIcon,
  ExternalLinkIcon,
  RefreshIcon,
} from '../components/ui/icons'
import { formatDateTime, formatNumber, humanize } from '../utils/format'
import type { JobMessageLogDto } from '../types/api'
import { DispatchPanel } from '../components/DispatchPanel'

const PAGE_SIZE = 10

export default function BackgroundJobsPage() {
  const dispatch = useAppDispatch()
  const { stats, schedules, messages, types, loading, error } = useAppSelector((state) => state.jobs)
  const { run, pending } = useMutation()

  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(1)
  const [dispatchType, setDispatchType] = useState('')
  const [dispatchProductId, setDispatchProductId] = useState('')

  const refresh = useCallback(() => dispatch(fetchJobOverview(50)), [dispatch])

  useEffect(() => {
    refresh()
  }, [refresh])

  // Auto-refresh so queue counters stay live while the page is open.
  useEffect(() => {
    const timer = window.setInterval(() => {
      dispatch(fetchJobOverview(50))
    }, 5000)
    return () => window.clearInterval(timer)
  }, [dispatch])

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase()
    return messages.filter((message) => {
      const matchesStatus = !statusFilter || message.status === statusFilter
      const matchesTerm =
        !term ||
        [message.messageType, message.reason, message.result, message.errorMessage, message.requestedBy]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(term))
      return matchesStatus && matchesTerm
    })
  }, [messages, query, statusFilter])

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, pageCount)
  const paged = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  const handleDispatch = async () => {
    if (!dispatchType) return
    const productId = dispatchProductId.trim() === '' ? undefined : Number(dispatchProductId)
    await run(dispatchJobMessage({ type: dispatchType, productId }), {
      successMessage: 'Message published to the broker.',
      onSuccess: () => {
        setDispatchProductId('')
        refresh()
      },
    })
  }

  const requiresProductId = dispatchType === 'LOW_STOCK_SCAN' || dispatchType === 'INVENTORY_SYNC'

  const columns: Column<JobMessageLogDto>[] = [
    {
      key: 'messageType',
      header: 'Message',
      render: (row) => (
        <span className="min-w-0">
          <span className="block truncate font-medium text-ink-900">{humanize(row.messageType)}</span>
          <span className="block truncate text-xs text-ink-500">{row.reason ?? 'No reason recorded'}</span>
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge value={row.status} />,
    },
    {
      key: 'result',
      header: 'Outcome',
      render: (row) =>
        row.errorMessage ? (
          <span className="text-xs text-red-600">{row.errorMessage}</span>
        ) : (
          <span className="text-xs text-ink-600">{row.result ?? '—'}</span>
        ),
    },
    {
      key: 'requestedBy',
      header: 'Producer',
      render: (row) => <span className="text-xs text-ink-500">{row.requestedBy ?? '—'}</span>,
    },
    {
      key: 'durationMillis',
      header: 'Duration',
      align: 'right',
      render: (row) => (
        <span className="text-xs text-ink-500">
          {row.durationMillis != null ? `${formatNumber(row.durationMillis)} ms` : '—'}
        </span>
      ),
    },
    {
      key: 'createdAt',
      header: 'Published',
      align: 'right',
      render: (row) => <span className="text-xs text-ink-500">{formatDateTime(row.createdAt)}</span>,
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="Background Jobs"
        description="JobRunr is the message broker: it persists every message in PostgreSQL and delivers it to a background worker."
        actions={
          <>
            <Button variant="secondary" icon={<RefreshIcon />} onClick={refresh} loading={loading}>
              Refresh
            </Button>
            {stats?.dashboardUrl && (
              <a href={stats.dashboardUrl} target="_blank" rel="noreferrer">
                <Button variant="ghost" icon={<ExternalLinkIcon />}>
                  JobRunr dashboard
                </Button>
              </a>
            )}
          </>
        }
      />

      {error && !stats ? (
        <div className="card">
          <ErrorState message={error} onRetry={refresh} />
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              label="In flight"
              value={formatNumber(stats?.pending ?? 0)}
              hint={`${formatNumber(stats?.enqueued ?? 0)} enqueued · ${formatNumber(stats?.processing ?? 0)} processing`}
              icon={<ActivityIcon />}
              tone="brand"
              loading={loading && !stats}
            />
            <StatCard
              label="Failed"
              value={formatNumber(stats?.failed ?? 0)}
              hint="Retried automatically with back-off"
              icon={<AlertTriangleIcon />}
              tone="red"
              loading={loading && !stats}
            />
            <StatCard
              label="Recurring schedules"
              value={formatNumber(stats?.recurringJobs ?? 0)}
              hint="Registered with the broker"
              icon={<ClockIcon />}
              tone="amber"
              loading={loading && !stats}
            />
            <StatCard
              label="Workers online"
              value={formatNumber(stats?.backgroundJobServers ?? 0)}
              hint="Background job servers"
              icon={<CpuIcon />}
              tone="emerald"
              loading={loading && !stats}
            />
          </div>

          <DispatchPanel
            types={types}
            schedules={schedules}
            dispatchType={dispatchType}
            onTypeChange={setDispatchType}
            productId={dispatchProductId}
            onProductIdChange={setDispatchProductId}
            requiresProductId={requiresProductId}
            pending={pending}
            onDispatch={handleDispatch}
          />

          <div>
            <h2 className="mb-3 text-sm font-semibold text-ink-900">Message history</h2>
            <Toolbar end={<span>{filtered.length} messages</span>}>
              <SearchBar value={query} onChange={setQuery} placeholder="Search message, reason or outcome…" />
              <div className="w-48">
                <SelectField value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                  <option value="">All statuses</option>
                  <option value="ENQUEUED">Enqueued</option>
                  <option value="RUNNING">Running</option>
                  <option value="SUCCEEDED">Succeeded</option>
                  <option value="FAILED">Failed</option>
                </SelectField>
              </div>
            </Toolbar>

            <DataTable
              columns={columns}
              rows={paged}
              rowKey={(row) => row.id ?? row.correlationId ?? JSON.stringify(row)}
              loading={loading && messages.length === 0}
              skeleton={<TableSkeleton columns={columns.length} />}
              caption="Broker message history"
              emptyState={
                <EmptyState
                  title="No messages yet"
                  description="Messages appear here as sales, purchases, returns and stock movements are processed."
                  icon={<ActivityIcon />}
                />
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
        </>
      )}
    </div>
  )
}
