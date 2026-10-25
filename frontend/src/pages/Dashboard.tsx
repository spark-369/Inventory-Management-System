import { useEffect, useMemo, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAppDispatch, useAppSelector, useMutation } from '../hooks'
import { fetchAlerts, generateAlerts } from '../store/alertsSlice'
import { fetchProducts } from '../store/productsSlice'
import { fetchSales } from '../store/salesSlice'
import { fetchPurchases } from '../store/purchasesSlice'
import {
  Badge,
  Button,
  EmptyState,
  ErrorState,
  PageHeader,
  Spinner,
  StatCard,
  StatusBadge,
} from '../components/ui'
import {
  AlertCircleIcon,
  AlertTriangleIcon,
  BoxIcon,
  CartIcon,
  LayersIcon,
  RefreshIcon,
  TrendUpIcon,
  TruckIcon,
} from '../components/ui/icons'
import { formatCurrency, formatDate, formatNumber, humanize } from '../utils/format'
import type { ProductDto } from '../types/api'

const LOW_STOCK_THRESHOLD = 5

export default function Dashboard() {
  const dispatch = useAppDispatch()
  const { items: alerts, loading: alertsLoading, error: alertsError } = useAppSelector((state) => state.alerts)
  const { items: products, loading: productsLoading } = useAppSelector((state) => state.products)
  const { items: sales, loading: salesLoading } = useAppSelector((state) => state.sales)
  const { items: purchases, loading: purchasesLoading } = useAppSelector((state) => state.purchases)
  const { run, pending } = useMutation()

  useEffect(() => {
    dispatch(fetchAlerts())
    dispatch(fetchProducts())
    dispatch(fetchSales())
    dispatch(fetchPurchases())
  }, [dispatch])

  const loading = productsLoading || salesLoading || purchasesLoading

  const lowStock = useMemo(
    () =>
      products
        .filter((p) => p.reorderLevel != null && (p.currentStock ?? 0) <= p.reorderLevel)
        .sort((a, b) => (a.currentStock ?? 0) - (b.currentStock ?? 0)),
    [products],
  )

  const inventoryValue = useMemo(
    () => products.reduce((total, product) => total + (product.purchasePrice ?? 0) * (product.currentStock ?? 0), 0),
    [products],
  )

  const revenue = useMemo(
    () =>
      sales
        .filter((sale) => sale.status === 'COMPLETED')
        .reduce((total, sale) => total + (sale.grandTotal ?? sale.totalAmount ?? 0), 0),
    [sales],
  )

  const pendingSales = sales.filter((sale) => sale.status === 'PENDING').length
  const pendingPurchases = purchases.filter((purchase) => purchase.status === 'PENDING').length

  const recentSales = useMemo(
    () => [...sales].sort((a, b) => (b.saleDate ?? '').localeCompare(a.saleDate ?? '')).slice(0, 5),
    [sales],
  )

  const handleGenerate = () => {
    void run(generateAlerts(), {
      successMessage: 'Alert scan queued — results appear shortly.',
      onSuccess: () => {
        dispatch(fetchAlerts())
      },
    })
  }

  const handleRefresh = () => {
    dispatch(fetchAlerts())
    dispatch(fetchProducts())
    dispatch(fetchSales())
    dispatch(fetchPurchases())
  }

  const criticalStock = lowStock.filter((p) => (p.currentStock ?? 0) <= LOW_STOCK_THRESHOLD)

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Live view of stock health, orders and alerts across every warehouse."
        actions={
          <>
            <Button variant="secondary" icon={<RefreshIcon />} onClick={handleRefresh} loading={loading}>
              Refresh
            </Button>
            <Button icon={<AlertTriangleIcon />} onClick={handleGenerate} loading={pending}>
              Run alert scan
            </Button>
          </>
        }
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Total products"
          value={formatNumber(products.length)}
          hint={`${lowStock.length} at or below reorder level`}
          icon={<BoxIcon />}
          tone="brand"
          loading={productsLoading}
        />
        <StatCard
          label="Inventory value"
          value={formatCurrency(inventoryValue)}
          hint="Based on purchase price"
          icon={<LayersIcon />}
          tone="emerald"
          loading={productsLoading}
        />
        <StatCard
          label="Completed revenue"
          value={formatCurrency(revenue)}
          hint={`${pendingSales} sale${pendingSales === 1 ? '' : 's'} awaiting completion`}
          icon={<TrendUpIcon />}
          tone="sky"
          loading={salesLoading}
        />
        <StatCard
          label="Open alerts"
          value={formatNumber(alerts.length)}
          hint={`${criticalStock.length} critically low item${criticalStock.length === 1 ? '' : 's'}`}
          icon={<AlertCircleIcon />}
          tone={alerts.length > 0 ? 'red' : 'ink'}
          loading={alertsLoading}
        />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <section className="card xl:col-span-2">
          <header className="flex items-center justify-between gap-4 border-b border-ink-200 px-5 py-4">
            <div>
              <h2 className="text-sm font-semibold text-ink-900">Low stock</h2>
              <p className="text-xs text-ink-500">Products at or below their reorder level</p>
            </div>
            <Link to="/products" className="text-sm font-medium text-brand-600 hover:text-brand-700">
              View all
            </Link>
          </header>

          {productsLoading ? (
            <div className="flex justify-center py-12">
              <Spinner className="text-ink-400" />
            </div>
          ) : lowStock.length === 0 ? (
            <EmptyState
              title="Stock levels are healthy"
              description="No product has dropped to its reorder level yet."
              icon={<BoxIcon />}
            />
          ) : (
            <ul className="divide-y divide-ink-100">
              {lowStock.slice(0, 6).map((product) => (
                <LowStockRow key={product.id} product={product} />
              ))}
            </ul>
          )}
        </section>

        <section className="card">
          <header className="flex items-center justify-between gap-4 border-b border-ink-200 px-5 py-4">
            <div>
              <h2 className="text-sm font-semibold text-ink-900">Recent alerts</h2>
              <p className="text-xs text-ink-500">Unresolved notifications</p>
            </div>
            <Link to="/alerts" className="text-sm font-medium text-brand-600 hover:text-brand-700">
              Manage
            </Link>
          </header>

          {alertsLoading ? (
            <div className="flex justify-center py-12">
              <Spinner className="text-ink-400" />
            </div>
          ) : alertsError ? (
            <ErrorState message={alertsError} onRetry={() => dispatch(fetchAlerts())} />
          ) : alerts.length === 0 ? (
            <EmptyState title="No open alerts" description="Everything is running smoothly." />
          ) : (
            <ul className="divide-y divide-ink-100">
              {alerts.slice(0, 6).map((alert) => (
                <li key={alert.id} className="flex items-start gap-3 px-5 py-3">
                  <Badge tone={alert.type === 'LOW_STOCK' ? 'warning' : 'danger'}>
                    {humanize(alert.type)}
                  </Badge>
                  <p className="min-w-0 flex-1 text-sm text-ink-700">{alert.message ?? '—'}</p>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section className="card">
        <header className="flex items-center justify-between gap-4 border-b border-ink-200 px-5 py-4">
          <div>
            <h2 className="text-sm font-semibold text-ink-900">Recent sales</h2>
            <p className="text-xs text-ink-500">Latest five transactions</p>
          </div>
          <Link to="/sales" className="text-sm font-medium text-brand-600 hover:text-brand-700">
            Open sales
          </Link>
        </header>

        {salesLoading ? (
          <div className="flex justify-center py-12">
            <Spinner className="text-ink-400" />
          </div>
        ) : recentSales.length === 0 ? (
          <EmptyState
            title="No sales recorded"
            description="Create your first sale to see it appear here."
            icon={<CartIcon />}
            action={
              <Link to="/sales">
                <Button size="sm" icon={<CartIcon />}>
                  New sale
                </Button>
              </Link>
            }
          />
        ) : (
          <ul className="divide-y divide-ink-100">
            {recentSales.map((sale) => (
              <li key={sale.id} className="flex flex-wrap items-center gap-3 px-5 py-3">
                <span className="w-28 shrink-0 truncate text-sm font-medium text-ink-900">
                  {sale.invoiceNumber ?? `#${sale.id}`}
                </span>
                <span className="min-w-0 flex-1 truncate text-sm text-ink-600">
                  {sale.customerName ?? 'Walk-in customer'}
                </span>
                <StatusBadge value={sale.status} />
                <span className="text-sm text-ink-500">{formatDate(sale.saleDate)}</span>
                <span className="w-24 text-right text-sm font-medium text-ink-900">
                  {formatCurrency(sale.grandTotal ?? sale.totalAmount)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <QuickLink
          to="/purchases"
          label="Purchases"
          value={pendingPurchases}
          hint="pending receipts"
          icon={<TruckIcon />}
        />
        <QuickLink to="/sales" label="Sales" value={pendingSales} hint="pending completion" icon={<CartIcon />} />
        <QuickLink to="/stock-batches" label="Stock batches" hint="track expiry & cost" icon={<LayersIcon />} />
        <QuickLink to="/alerts" label="Alerts" value={alerts.length} hint="unresolved" icon={<AlertTriangleIcon />} />
      </div>
    </div>
  )
}

function LowStockRow({ product }: { product: ProductDto }) {
  const stock = product.currentStock ?? 0
  const reorder = product.reorderLevel ?? 0
  const percentage = reorder > 0 ? Math.min(100, Math.round((stock / reorder) * 100)) : 0
  const critical = stock <= LOW_STOCK_THRESHOLD

  return (
    <li className="flex flex-wrap items-center gap-3 px-5 py-3">
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-ink-900">{product.name}</p>
        <p className="truncate text-xs text-ink-500">{product.sku}</p>
      </div>
      <div className="w-32">
        <div className="h-1.5 overflow-hidden rounded-full bg-ink-100">
          <div
            className={`h-full rounded-full ${critical ? 'bg-red-500' : 'bg-amber-500'}`}
            style={{ width: `${percentage}%` }}
          />
        </div>
      </div>
      <Badge tone={critical ? 'danger' : 'warning'}>
        {formatNumber(stock)} / {formatNumber(reorder)}
      </Badge>
    </li>
  )
}

function QuickLink({
  to,
  label,
  value,
  hint,
  icon,
}: {
  to: string
  label: string
  value?: number
  hint: string
  icon: ReactNode
}) {
  return (
    <Link to={to} className="card flex items-center gap-4 p-4 transition hover:border-brand-300 hover:shadow-md">
      <span className="flex size-10 items-center justify-center rounded-lg bg-brand-50 text-xl text-brand-600">
        {icon}
      </span>
      <span className="min-w-0">
        <span className="block text-sm font-medium text-ink-900">{label}</span>
        <span className="block text-xs text-ink-500">
          {value !== undefined ? `${value} ${hint}` : hint}
        </span>
      </span>
    </Link>
  )
}
