import { useAppSelector } from '../hooks'
import { CrudPage } from '../components/CrudPage'
import type { CrudField } from '../components/CrudField'
import {
  createStockBatch,
  deleteStockBatch,
  fetchStockBatches,
  updateStockBatch,
} from '../store/stockBatchesSlice'
import { fetchProducts } from '../store/productsSlice'
import { fetchWarehouses } from '../store/warehousesSlice'
import { Badge, StatusBadge } from '../components/ui'
import { LayersIcon } from '../components/ui/icons'
import { daysUntil, formatCurrency, formatDate, formatNumber } from '../utils/format'
import type { StockBatchDto } from '../types/api'

interface Form extends Record<string, unknown> {
  productId: number | undefined
  warehouseId: number | undefined
  batchNumber: string
  expiryDate: string
  quantity: number | undefined
  costPrice: number | undefined
  active: boolean
}

const emptyForm: Form = {
  productId: undefined,
  warehouseId: undefined,
  batchNumber: '',
  expiryDate: '',
  quantity: undefined,
  costPrice: undefined,
  active: true,
}

export default function StockBatchesPage() {
  const { items, loading, error } = useAppSelector((state) => state.stockBatches)
  const { items: products } = useAppSelector((state) => state.products)
  const { items: warehouses } = useAppSelector((state) => state.warehouses)

  const refresh = () => [fetchStockBatches(), fetchProducts(), fetchWarehouses()]

  const fields: CrudField[] = [
    {
      name: 'productId',
      label: 'Product',
      type: 'select',
      required: true,
      options: products.map((p) => ({ value: p.id!, label: `${p.name} (${p.sku})` })),
    },
    {
      name: 'warehouseId',
      label: 'Warehouse',
      type: 'select',
      required: true,
      options: warehouses.map((w) => ({ value: w.id!, label: w.name })),
    },
    { name: 'batchNumber', label: 'Batch number', required: true, placeholder: 'e.g. LOT-2026-01' },
    { name: 'expiryDate', label: 'Expiry date', type: 'date', hint: 'Leave blank for non-perishable goods.' },
    { name: 'quantity', label: 'Quantity', type: 'number', step: '1', required: true },
    { name: 'costPrice', label: 'Cost price', type: 'number' },
    { name: 'active', label: 'Active', type: 'checkbox', hint: 'Inactive batches are excluded from stock.' },
  ]

  return (
    <CrudPage<StockBatchDto, Form>
      title="Stock Batches"
      description="Track batch numbers, expiry dates and cost prices for perishable stock."
      entityLabel="batch"
      entityLabelPlural="batches"
      icon={<LayersIcon />}
      rows={items}
      loading={loading}
      error={error}
      onRefresh={refresh}
      fields={fields}
      emptyForm={emptyForm}
      emptyDescription="Create a batch to track expiry dates and per-batch costs."
      toForm={(row) => ({
        productId: row.productId,
        warehouseId: row.warehouseId,
        batchNumber: row.batchNumber ?? '',
        expiryDate: row.expiryDate ?? '',
        quantity: row.quantity,
        costPrice: row.costPrice,
        active: row.active ?? true,
      })}
      toPayload={(form) => ({
        productId: form.productId,
        warehouseId: form.warehouseId,
        batchNumber: form.batchNumber.trim(),
        expiryDate: form.expiryDate || undefined,
        quantity: form.quantity,
        costPrice: form.costPrice ?? undefined,
        active: form.active,
      })}
      onCreate={createStockBatch}
      onUpdate={updateStockBatch}
      onDelete={deleteStockBatch}
      searchable={(row) => [row.productName, row.warehouseName, row.batchNumber]}
      sortValues={{
        batchNumber: (row) => row.batchNumber,
        expiryDate: (row) => row.expiryDate,
        quantity: (row) => row.quantity,
      }}
      columns={[
        {
          key: 'product',
          header: 'Product',
          sortValue: (row) => row.productName,
          render: (row) => (
            <span className="min-w-0">
              <span className="block truncate font-medium text-ink-900">{row.productName ?? '—'}</span>
              <span className="block truncate text-xs text-ink-500">{row.batchNumber ?? 'No batch number'}</span>
            </span>
          ),
        },
        {
          key: 'warehouse',
          header: 'Warehouse',
          sortValue: (row) => row.warehouseName,
          render: (row) => <span className="text-ink-600">{row.warehouseName ?? '—'}</span>,
        },
        {
          key: 'expiryDate',
          header: 'Expiry',
          sortValue: (row) => row.expiryDate,
          render: (row) => {
            const remaining = daysUntil(row.expiryDate)
            if (remaining === null) return <span className="text-ink-400">—</span>
            const tone = remaining < 0 ? 'danger' : remaining <= 7 ? 'warning' : 'neutral'
            const label = remaining < 0 ? 'Expired' : `${remaining} day${remaining === 1 ? '' : 's'} left`
            return (
              <div className="flex flex-col gap-1">
                <span className="text-ink-600">{formatDate(row.expiryDate)}</span>
                <Badge tone={tone}>{label}</Badge>
              </div>
            )
          },
        },
        {
          key: 'quantity',
          header: 'Qty',
          align: 'right',
          sortValue: (row) => row.quantity,
          render: (row) => <span className="font-medium text-ink-900">{formatNumber(row.quantity)}</span>,
        },
        {
          key: 'costPrice',
          header: 'Cost price',
          align: 'right',
          render: (row) => <span className="text-ink-700">{formatCurrency(row.costPrice)}</span>,
        },
        {
          key: 'active',
          header: 'Status',
          render: (row) => <StatusBadge value={row.active ? 'ACTIVE' : 'DISABLED'} />,
        },
      ]}
    />
  )
}
