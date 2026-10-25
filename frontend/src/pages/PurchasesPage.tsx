import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useAppDispatch, useAppSelector, useConfirm, useMutation } from '../hooks'
import { createPurchase, deletePurchase, fetchPurchases, receivePurchase, updatePurchase } from '../store/purchasesSlice'
import { fetchSuppliers } from '../store/suppliersSlice'
import { fetchWarehouses } from '../store/warehousesSlice'
import { fetchProducts } from '../store/productsSlice'
import {
  Button,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  Modal,
  PageHeader,
  Pagination,
  SearchBar,
  SelectField,
  StatusBadge,
  TableSkeleton,
  TextAreaField,
  Toolbar,
  type Column,
} from '../components/ui'
import { LineItemsEditor, type LineItem } from '../components/LineItemsEditor'
import { CheckIcon, PencilIcon, PlusIcon, TrashIcon, TruckIcon } from '../components/ui/icons'
import { formatCurrency, formatDate } from '../utils/format'
import type { PurchaseDto } from '../types/api'

interface FormState {
  supplierId: number | undefined
  warehouseId: number | undefined
  notes: string
  items: LineItem[]
}

const emptyForm: FormState = { supplierId: undefined, warehouseId: undefined, notes: '', items: [] }
const PAGE_SIZE = 10

export default function PurchasesPage() {
  const dispatch = useAppDispatch()
  const { items: purchases, loading, error } = useAppSelector((state) => state.purchases)
  const { items: suppliers } = useAppSelector((state) => state.suppliers)
  const { items: warehouses } = useAppSelector((state) => state.warehouses)
  const { items: products } = useAppSelector((state) => state.products)
  const { run, pending } = useMutation()
  const { confirm, confirmState, handleConfirm, handleCancel } = useConfirm()

  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(1)

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editing, setEditing] = useState<PurchaseDto | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [formError, setFormError] = useState<string>()

  useEffect(() => {
    dispatch(fetchPurchases())
    dispatch(fetchSuppliers())
    dispatch(fetchWarehouses())
    dispatch(fetchProducts())
  }, [dispatch])

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase()
    return purchases.filter((purchase) => {
      const matchesStatus = !statusFilter || purchase.status === statusFilter
      const matchesTerm =
        !term ||
        [purchase.purchaseOrderNumber, purchase.supplierName, purchase.warehouseName]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(term))
      return matchesStatus && matchesTerm
    })
  }, [purchases, query, statusFilter])

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, pageCount)
  const paged = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  const openCreate = () => {
    setEditing(null)
    setForm(emptyForm)
    setFormError(undefined)
    setIsModalOpen(true)
  }

  const openEdit = (purchase: PurchaseDto) => {
    setEditing(purchase)
    setForm({
      supplierId: purchase.supplierId,
      warehouseId: purchase.warehouseId,
      notes: purchase.notes ?? '',
      items: (purchase.items ?? []).map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        batchNumber: item.batchNumber,
      })),
    })
    setFormError(undefined)
    setIsModalOpen(true)
  }

  const closeModal = () => {
    setIsModalOpen(false)
    setEditing(null)
    setForm(emptyForm)
    setFormError(undefined)
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!form.supplierId) {
      setFormError('Select a supplier for this purchase order.')
      return
    }
    if (!form.warehouseId) {
      setFormError('Select the destination warehouse.')
      return
    }
    const validItems = form.items.filter((item) => item.productId > 0 && item.quantity > 0)
    if (validItems.length === 0) {
      setFormError('Add at least one product with a quantity greater than zero.')
      return
    }

    const payload: PurchaseDto = {
      supplierId: form.supplierId,
      warehouseId: form.warehouseId,
      notes: form.notes.trim() || undefined,
      items: validItems,
    }

    const result = editing?.id
      ? await run(updatePurchase({ id: editing.id, data: payload }), { successMessage: 'Purchase updated.' })
      : await run(createPurchase(payload), { successMessage: 'Purchase order created.' })

    if (result !== undefined) closeModal()
  }

  const handleReceive = async (purchase: PurchaseDto) => {
    const confirmed = await confirm({
      title: 'Receive purchase',
      message: (
        <>
          Receiving <strong>{purchase.purchaseOrderNumber ?? `order #${purchase.id}`}</strong> will add
          the ordered quantities to stock and clear any matching low-stock alerts.
        </>
      ),
      confirmLabel: 'Receive stock',
      tone: 'primary',
    })
    if (confirmed && purchase.id) {
      await run(receivePurchase(purchase.id), { successMessage: 'Purchase received and stock updated.' })
    }
  }

  const handleDelete = async (purchase: PurchaseDto) => {
    const confirmed = await confirm({
      title: 'Delete purchase',
      message: (
        <>
          Are you sure you want to delete{' '}
          <strong>{purchase.purchaseOrderNumber ?? `order #${purchase.id}`}</strong>? This action cannot
          be undone.
        </>
      ),
      confirmLabel: 'Delete',
    })
    if (confirmed && purchase.id) {
      await run(deletePurchase(purchase.id), { successMessage: 'Purchase deleted.' })
    }
  }

  const columns: Column<PurchaseDto>[] = [
    {
      key: 'order',
      header: 'Order',
      render: (row) => (
        <span className="min-w-0">
          <span className="block truncate font-medium text-ink-900">
            {row.purchaseOrderNumber ?? `#${row.id}`}
          </span>
          <span className="block truncate text-xs text-ink-500">{formatDate(row.purchaseDate)}</span>
        </span>
      ),
    },
    {
      key: 'supplier',
      header: 'Supplier',
      render: (row) => <span className="text-ink-600">{row.supplierName ?? '—'}</span>,
    },
    {
      key: 'warehouse',
      header: 'Warehouse',
      render: (row) => <span className="text-ink-600">{row.warehouseName ?? '—'}</span>,
    },
    {
      key: 'items',
      header: 'Items',
      align: 'right',
      render: (row) => <span className="text-ink-600">{row.items?.length ?? 0}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge value={row.status} />,
    },
    {
      key: 'totalAmount',
      header: 'Total',
      align: 'right',
      render: (row) => <span className="font-medium text-ink-900">{formatCurrency(row.totalAmount)}</span>,
    },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (row) => (
        <div className="flex items-center justify-end gap-1">
          {row.status === 'PENDING' && (
            <Button
              size="sm"
              variant="success"
              icon={<CheckIcon />}
              onClick={() => void handleReceive(row)}
              disabled={pending}
            >
              Receive
            </Button>
          )}
          <Button size="icon" variant="ghost" aria-label="Edit purchase" onClick={() => openEdit(row)}>
            <PencilIcon />
          </Button>
          <Button
            size="icon"
            variant="ghost"
            className="text-red-600 hover:bg-red-50"
            aria-label="Delete purchase"
            onClick={() => void handleDelete(row)}
          >
            <TrashIcon />
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="Purchases"
        description="Raise supplier purchase orders and receive stock into a warehouse."
        actions={
          <Button icon={<PlusIcon />} onClick={openCreate}>
            New purchase order
          </Button>
        }
      />

      <Toolbar end={<span>{filtered.length} orders</span>}>
        <SearchBar value={query} onChange={setQuery} placeholder="Search order or supplier…" />
        <div className="w-44">
          <SelectField value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="">All statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="PENDING">Pending</option>
            <option value="RECEIVED">Received</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </SelectField>
        </div>
      </Toolbar>

      <DataTable
        columns={columns}
        rows={paged}
        rowKey={(row) => row.id!}
        loading={loading}
        skeleton={<TableSkeleton columns={columns.length} />}
        caption="Purchase orders list"
        emptyState={
          error ? (
            <ErrorState message={error} onRetry={() => dispatch(fetchPurchases())} />
          ) : filtered.length === 0 && (query || statusFilter) ? (
            <EmptyState
              title="No matches"
              description="No purchase order matches the current filters."
              icon={<TruckIcon />}
            />
          ) : (
            <EmptyState
              title="No purchase orders yet"
              description="Raise your first order to start receiving stock."
              icon={<TruckIcon />}
              action={
                <Button size="sm" icon={<PlusIcon />} onClick={openCreate}>
                  New purchase order
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

      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editing ? 'Edit purchase order' : 'New purchase order'}
        description={
          editing
            ? 'Update the order header and line items.'
            : 'Select a supplier and warehouse, then add the products you are ordering.'
        }
        size="xl"
        footer={
          <>
            <Button variant="secondary" onClick={closeModal} disabled={pending}>
              Cancel
            </Button>
            <Button type="submit" form="purchase-form" loading={pending}>
              {editing ? 'Save changes' : 'Create order'}
            </Button>
          </>
        }
      >
        <form id="purchase-form" onSubmit={handleSubmit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField
            label="Supplier"
            required
            value={form.supplierId ?? ''}
            onChange={(event) =>
              setForm({ ...form, supplierId: event.target.value ? Number(event.target.value) : undefined })
            }
          >
            <option value="">Select supplier…</option>
            {suppliers.map((supplier) => (
              <option key={supplier.id} value={supplier.id}>
                {supplier.name}
              </option>
            ))}
          </SelectField>

          <SelectField
            label="Warehouse"
            required
            value={form.warehouseId ?? ''}
            onChange={(event) =>
              setForm({ ...form, warehouseId: event.target.value ? Number(event.target.value) : undefined })
            }
          >
            <option value="">Select warehouse…</option>
            {warehouses.map((warehouse) => (
              <option key={warehouse.id} value={warehouse.id}>
                {warehouse.name}
              </option>
            ))}
          </SelectField>

          <TextAreaField
            label="Notes"
            value={form.notes}
            onChange={(event) => setForm({ ...form, notes: event.target.value })}
            placeholder="Optional internal notes"
          />

          <LineItemsEditor
            items={form.items}
            products={products}
            priceSource="purchase"
            error={formError}
            onChange={(items) => {
              setForm({ ...form, items })
              if (formError) setFormError(undefined)
            }}
          />
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={confirmState.isOpen}
        title={confirmState.title}
        message={confirmState.message}
        confirmLabel={confirmState.confirmLabel}
        cancelLabel={confirmState.cancelLabel}
        tone={confirmState.tone}
        loading={pending}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    </div>
  )
}
