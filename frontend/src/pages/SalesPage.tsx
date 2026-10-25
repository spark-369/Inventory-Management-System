import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useAppDispatch, useAppSelector, useConfirm, useMutation } from '../hooks'
import { completeSale, createSale, deleteSale, fetchSales, updateSale } from '../store/salesSlice'
import { fetchCustomers } from '../store/customersSlice'
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
  TextField,
  Toolbar,
  type Column,
} from '../components/ui'
import { LineItemsEditor, type LineItem } from '../components/LineItemsEditor'
import { CartIcon, CheckIcon, PencilIcon, PlusIcon, TrashIcon } from '../components/ui/icons'
import { formatCurrency, formatDate } from '../utils/format'
import type { SaleDto } from '../types/api'

interface FormState {
  customerId: number | undefined
  warehouseId: number | undefined
  discount: number | undefined
  taxAmount: number | undefined
  notes: string
  items: LineItem[]
}

const emptyForm: FormState = {
  customerId: undefined,
  warehouseId: undefined,
  discount: undefined,
  taxAmount: undefined,
  notes: '',
  items: [],
}

const PAGE_SIZE = 10

export default function SalesPage() {
  const dispatch = useAppDispatch()
  const { items: sales, loading, error } = useAppSelector((state) => state.sales)
  const { items: customers } = useAppSelector((state) => state.customers)
  const { items: warehouses } = useAppSelector((state) => state.warehouses)
  const { items: products } = useAppSelector((state) => state.products)
  const { run, pending } = useMutation()
  const { confirm, confirmState, handleConfirm, handleCancel } = useConfirm()

  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(1)

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editing, setEditing] = useState<SaleDto | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [formError, setFormError] = useState<string>()

  useEffect(() => {
    dispatch(fetchSales())
    dispatch(fetchCustomers())
    dispatch(fetchWarehouses())
    dispatch(fetchProducts())
  }, [dispatch])

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase()
    return sales.filter((sale) => {
      const matchesStatus = !statusFilter || sale.status === statusFilter
      const matchesTerm =
        !term ||
        [sale.invoiceNumber, sale.customerName, sale.warehouseName]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(term))
      return matchesStatus && matchesTerm
    })
  }, [sales, query, statusFilter])

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, pageCount)
  const paged = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  const openCreate = () => {
    setEditing(null)
    setForm(emptyForm)
    setFormError(undefined)
    setIsModalOpen(true)
  }

  const openEdit = (sale: SaleDto) => {
    setEditing(sale)
    setForm({
      customerId: sale.customerId,
      warehouseId: sale.warehouseId,
      discount: sale.discount,
      taxAmount: sale.taxAmount,
      notes: sale.notes ?? '',
      items: (sale.items ?? []).map((item) => ({
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
    if (!form.warehouseId) {
      setFormError('Select a warehouse for this sale.')
      return
    }
    const validItems = form.items.filter((item) => item.productId > 0 && item.quantity > 0)
    if (validItems.length === 0) {
      setFormError('Add at least one product with a quantity greater than zero.')
      return
    }

    const payload: SaleDto = {
      customerId: form.customerId,
      warehouseId: form.warehouseId,
      discount: form.discount,
      taxAmount: form.taxAmount,
      notes: form.notes.trim() || undefined,
      items: validItems,
    }

    const result = editing?.id
      ? await run(updateSale({ id: editing.id, data: payload }), { successMessage: 'Sale updated.' })
      : await run(createSale(payload), { successMessage: 'Sale created.' })

    if (result !== undefined) closeModal()
  }

  const handleComplete = async (sale: SaleDto) => {
    const confirmed = await confirm({
      title: 'Complete sale',
      message: (
        <>
          Completing <strong>{sale.invoiceNumber ?? `sale #${sale.id}`}</strong> will deduct the sold
          quantities from stock and may raise low-stock alerts.
        </>
      ),
      confirmLabel: 'Complete sale',
      tone: 'primary',
    })
    if (confirmed && sale.id) {
      await run(completeSale(sale.id), { successMessage: 'Sale completed and stock updated.' })
    }
  }

  const handleDelete = async (sale: SaleDto) => {
    const confirmed = await confirm({
      title: 'Delete sale',
      message: (
        <>
          Are you sure you want to delete <strong>{sale.invoiceNumber ?? `sale #${sale.id}`}</strong>?
          This action cannot be undone.
        </>
      ),
      confirmLabel: 'Delete',
    })
    if (confirmed && sale.id) {
      await run(deleteSale(sale.id), { successMessage: 'Sale deleted.' })
    }
  }

  const columns: Column<SaleDto>[] = [
    {
      key: 'invoice',
      header: 'Invoice',
      render: (row) => (
        <span className="min-w-0">
          <span className="block truncate font-medium text-ink-900">
            {row.invoiceNumber ?? `#${row.id}`}
          </span>
          <span className="block truncate text-xs text-ink-500">{formatDate(row.saleDate)}</span>
        </span>
      ),
    },
    {
      key: 'customer',
      header: 'Customer',
      render: (row) => <span className="text-ink-600">{row.customerName ?? 'Walk-in'}</span>,
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
      key: 'grandTotal',
      header: 'Grand total',
      align: 'right',
      render: (row) => (
        <span className="font-medium text-ink-900">{formatCurrency(row.grandTotal ?? row.totalAmount)}</span>
      ),
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
              onClick={() => void handleComplete(row)}
              disabled={pending}
            >
              Complete
            </Button>
          )}
          <Button size="icon" variant="ghost" aria-label="Edit sale" onClick={() => openEdit(row)}>
            <PencilIcon />
          </Button>
          <Button
            size="icon"
            variant="ghost"
            className="text-red-600 hover:bg-red-50"
            aria-label="Delete sale"
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
        title="Sales"
        description="Create customer sales, complete them to deduct stock, and review invoices."
        actions={
          <Button icon={<PlusIcon />} onClick={openCreate}>
            New sale
          </Button>
        }
      />

      <Toolbar end={<span>{filtered.length} sales</span>}>
        <SearchBar value={query} onChange={setQuery} placeholder="Search invoice or customer…" />
        <div className="w-44">
          <SelectField value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="">All statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="PENDING">Pending</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="RETURNED">Returned</option>
          </SelectField>
        </div>
      </Toolbar>

      <DataTable
        columns={columns}
        rows={paged}
        rowKey={(row) => row.id!}
        loading={loading}
        skeleton={<TableSkeleton columns={columns.length} />}
        caption="Sales list"
        emptyState={
          error ? (
            <ErrorState message={error} onRetry={() => dispatch(fetchSales())} />
          ) : filtered.length === 0 && (query || statusFilter) ? (
            <EmptyState
              title="No matches"
              description="No sale matches the current filters."
              icon={<CartIcon />}
            />
          ) : (
            <EmptyState
              title="No sales yet"
              description="Record your first sale to start tracking revenue."
              icon={<CartIcon />}
              action={
                <Button size="sm" icon={<PlusIcon />} onClick={openCreate}>
                  New sale
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
        title={editing ? 'Edit sale' : 'New sale'}
        description={
          editing
            ? 'Update the sale header and line items.'
            : 'Add products and quantities, then save to create a pending sale.'
        }
        size="xl"
        footer={
          <>
            <Button variant="secondary" onClick={closeModal} disabled={pending}>
              Cancel
            </Button>
            <Button type="submit" form="sale-form" loading={pending}>
              {editing ? 'Save changes' : 'Create sale'}
            </Button>
          </>
        }
      >
        <form id="sale-form" onSubmit={handleSubmit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField
            label="Customer"
            value={form.customerId ?? ''}
            onChange={(event) =>
              setForm({ ...form, customerId: event.target.value ? Number(event.target.value) : undefined })
            }
            hint="Leave blank for a walk-in customer."
          >
            <option value="">Walk-in customer</option>
            {customers.map((customer) => (
              <option key={customer.id} value={customer.id}>
                {customer.name}
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

          <TextField
            label="Discount"
            type="number"
            step="0.01"
            value={form.discount ?? ''}
            onChange={(event) =>
              setForm({ ...form, discount: event.target.value === '' ? undefined : Number(event.target.value) })
            }
            hint="Subtracted from the line-item total."
          />

          <TextField
            label="Tax amount"
            type="number"
            step="0.01"
            value={form.taxAmount ?? ''}
            onChange={(event) =>
              setForm({ ...form, taxAmount: event.target.value === '' ? undefined : Number(event.target.value) })
            }
            hint="Added on top of the line-item total."
          />

          <TextAreaField
            label="Notes"
            value={form.notes}
            onChange={(event) => setForm({ ...form, notes: event.target.value })}
            placeholder="Optional internal notes"
          />

          <LineItemsEditor
            items={form.items}
            products={products}
            priceSource="selling"
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
