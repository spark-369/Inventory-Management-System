import { useAppSelector } from '../hooks'
import { CrudPage } from '../components/CrudPage'
import type { CrudField } from '../components/CrudField'
import { createStockMovement, fetchStockMovements } from '../store/stockMovementsSlice'
import { fetchProducts } from '../store/productsSlice'
import { fetchWarehouses } from '../store/warehousesSlice'
import { StatusBadge } from '../components/ui'
import { SwapIcon } from '../components/ui/icons'
import { formatDateTime, formatNumber, humanize } from '../utils/format'
import type { StockMovementDto } from '../types/api'

interface Form extends Record<string, unknown> {
  productId: number | undefined
  warehouseId: number | undefined
  movementType: string
  quantity: number | undefined
  referenceType: string
  notes: string
}

const emptyForm: Form = {
  productId: undefined,
  warehouseId: undefined,
  movementType: 'IN',
  quantity: undefined,
  referenceType: 'SYSTEM',
  notes: '',
}

const movementOptions = [
  { value: 'IN', label: 'IN — add stock' },
  { value: 'OUT', label: 'OUT — remove stock' },
  { value: 'ADJUSTMENT', label: 'ADJUSTMENT — set exact stock' },
  { value: 'TRANSFER_IN', label: 'TRANSFER IN' },
  { value: 'TRANSFER_OUT', label: 'TRANSFER OUT' },
  { value: 'RETURN', label: 'RETURN' },
  { value: 'SALE', label: 'SALE' },
  { value: 'PURCHASE', label: 'PURCHASE' },
]

const referenceOptions = [
  { value: 'SYSTEM', label: 'System' },
  { value: 'PURCHASE', label: 'Purchase' },
  { value: 'SALE', label: 'Sale' },
  { value: 'TRANSFER', label: 'Transfer' },
  { value: 'ADJUSTMENT', label: 'Adjustment' },
  { value: 'RETURN', label: 'Return' },
]

export default function StockMovementsPage() {
  const { items, loading, error } = useAppSelector((state) => state.stockMovements)
  const { items: products } = useAppSelector((state) => state.products)
  const { items: warehouses } = useAppSelector((state) => state.warehouses)

  const refresh = () => [fetchStockMovements(), fetchProducts(), fetchWarehouses()]

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
    {
      name: 'movementType',
      label: 'Movement type',
      type: 'select',
      required: true,
      options: movementOptions,
      hint: 'ADJUSTMENT sets the stock level to the quantity entered.',
    },
    {
      name: 'quantity',
      label: 'Quantity',
      type: 'number',
      step: '1',
      required: true,
      validate: (value) => (Number(value) <= 0 ? 'Quantity must be greater than zero.' : undefined),
    },
    { name: 'referenceType', label: 'Reference type', type: 'select', options: referenceOptions },
    { name: 'notes', label: 'Notes', type: 'textarea', placeholder: 'Why was this movement recorded?' },
  ]

  return (
    <CrudPage<StockMovementDto, Form>
      title="Stock Movements"
      description="Immutable audit trail of every stock change, plus manual adjustments."
      entityLabel="movement"
      entityLabelPlural="movements"
      icon={<SwapIcon />}
      rows={items}
      loading={loading}
      error={error}
      onRefresh={refresh}
      fields={fields}
      emptyForm={emptyForm}
      canEdit={false}
      canDelete={false}
      emptyDescription="Movements appear here automatically as stock is received, sold or adjusted."
      toForm={(row) => ({
        productId: row.productId,
        warehouseId: row.warehouseId,
        movementType: row.movementType ?? 'IN',
        quantity: row.quantity,
        referenceType: row.referenceType ?? 'SYSTEM',
        notes: row.notes ?? '',
      })}
      toPayload={(form) => ({
        productId: form.productId,
        warehouseId: form.warehouseId,
        movementType: form.movementType,
        quantity: form.quantity,
        referenceType: form.referenceType,
        notes: form.notes.trim() || undefined,
      })}
      onCreate={createStockMovement}
      searchable={(row) => [row.productName, row.warehouseName, row.referenceType, row.notes]}
      sortValues={{
        createdAt: (row) => row.createdAt,
        productName: (row) => row.productName,
        quantity: (row) => row.quantity,
      }}
      columns={[
        {
          key: 'createdAt',
          header: 'When',
          sortValue: (row) => row.createdAt,
          render: (row) => (
            <span className="whitespace-nowrap text-ink-600">{formatDateTime(row.createdAt)}</span>
          ),
        },
        {
          key: 'productName',
          header: 'Product',
          sortValue: (row) => row.productName,
          render: (row) => <span className="font-medium text-ink-900">{row.productName ?? '—'}</span>,
        },
        {
          key: 'warehouseName',
          header: 'Warehouse',
          sortValue: (row) => row.warehouseName,
          render: (row) => <span className="text-ink-600">{row.warehouseName ?? '—'}</span>,
        },
        {
          key: 'movementType',
          header: 'Type',
          render: (row) => <StatusBadge value={row.movementType} />,
        },
        {
          key: 'quantity',
          header: 'Qty',
          align: 'right',
          sortValue: (row) => row.quantity,
          render: (row) => <span className="font-medium text-ink-900">{formatNumber(row.quantity)}</span>,
        },
        {
          key: 'referenceType',
          header: 'Reference',
          render: (row) => <span className="text-ink-600">{humanize(row.referenceType)}</span>,
        },
        {
          key: 'notes',
          header: 'Notes',
          render: (row) => (
            <span className="block max-w-xs truncate text-ink-500" title={row.notes ?? ''}>
              {row.notes || '—'}
            </span>
          ),
        },
      ]}
    />
  )
}
