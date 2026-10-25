import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useAppDispatch, useAppSelector, useConfirm, useMutation } from '../hooks'
import { approveReturn, createReturn, deleteReturn, fetchReturns, updateReturn } from '../store/returnsSlice'
import { fetchProducts } from '../store/productsSlice'
import { fetchWarehouses } from '../store/warehousesSlice'
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
import { CheckIcon, PencilIcon, PlusIcon, TrashIcon, UndoIcon } from '../components/ui/icons'
import { formatDate } from '../utils/format'
import type { ReturnDto } from '../types/api'

interface FormState {
  type: string
  productId: number | undefined
  warehouseId: number | undefined
  quantity: number | undefined
  batchNumber: string
  unitPrice: number | undefined
  totalAmount: number | undefined
  reason: string
}

const emptyForm: FormState = {
  type: 'PURCHASE_RETURN',
  productId: undefined,
  warehouseId: undefined,
  quantity: undefined,
  batchNumber: '',
  unitPrice: undefined,
  totalAmount: undefined,
  reason: '',
}

const PAGE_SIZE = 10

export default function ReturnsPage() {
  const dispatch = useAppDispatch()
  const { items: returns, loading, error } = useAppSelector((state) => state.returns)
  const { items: products } = useAppSelector((state) => state.products)
  const { items: warehouses } = useAppSelector((state) => state.warehouses)
  const { run, pending } = useMutation()
  const { confirm, confirmState, handleConfirm, handleCancel } = useConfirm()

  const [query, setQuery] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [page, setPage] = useState(1)

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editing, setEditing] = useState<ReturnDto | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [errors, setErrors] = useState<Record<string, string | undefined>>({})

  useEffect(() => {
    dispatch(fetchReturns())
    dispatch(fetchProducts())
    dispatch(fetchWarehouses())
  }, [dispatch])

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase()
    return returns.filter((item) => {
      const matchesType = !typeFilter || item.type === typeFilter
      const matchesTerm =
        !term ||
        [item.returnNumber, item.productName, item.warehouseName, item.reason]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(term))
      return matchesType && matchesTerm
    })
  }, [returns, query, typeFilter])

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, pageCount)
  const paged = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  const openCreate = () => {
    setEditing(null)
    setForm(emptyForm)
    setErrors({})
    setIsModalOpen(true)
  }

  const openEdit = (item: ReturnDto) => {
    setEditing(item)
    setForm({
      type: item.type ?? 'PURCHASE_RETURN',
      productId: item.productId,
      warehouseId: item.warehouseId,
      quantity: item.quantity,
      batchNumber: item.batchNumber ?? '',
      unitPrice: item.unitPrice,
      totalAmount: item.totalAmount,
      reason: item.reason ?? '',
    })
    setErrors({})
    setIsModalOpen(true)
  }

  const closeModal = () => {
    setIsModalOpen(false)
    setEditing(null)
    setForm(emptyForm)
    setErrors({})
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const nextErrors: Record<string, string | undefined> = {}
    if (!form.productId) nextErrors.productId = 'Select a product.'
    if (!form.warehouseId) nextErrors.warehouseId = 'Select a warehouse.'
    if (!form.quantity || form.quantity <= 0) nextErrors.quantity = 'Quantity must be greater than zero.'
    setErrors(nextErrors)
    if (Object.values(nextErrors).some(Boolean)) return

    const payload: ReturnDto = {
      type: form.type,
      productId: form.productId!,
      warehouseId: form.warehouseId!,
      quantity: form.quantity,
      batchNumber: form.batchNumber.trim() || undefined,
      unitPrice: form.unitPrice,
      totalAmount: form.totalAmount,
      reason: form.reason.trim() || undefined,
    }

    const result = editing?.id
      ? await run(updateReturn({ id: editing.id, data: payload }), { successMessage: 'Return updated.' })
      : await run(createReturn(payload), { successMessage: 'Return created.' })

    if (result !== undefined) closeModal()
  }

  const handleApprove = async (item: ReturnDto) => {
    const effect =
      item.type === 'PURCHASE_RETURN'
        ? 'remove the returned quantity from stock'
        : 'add the returned quantity back to stock'
    const confirmed = await confirm({
      title: 'Approve return',
      message: (
        <>
          Approving <strong>{item.returnNumber ?? `return #${item.id}`}</strong> will {effect}.
        </>
      ),
      confirmLabel: 'Approve',
      tone: 'primary',
    })
    if (confirmed && item.id) {
      await run(approveReturn(item.id), { successMessage: 'Return approved and stock adjusted.' })
    }
  }

  const handleDelete = async (item: ReturnDto) => {
    const confirmed = await confirm({
      title: 'Delete return',
      message: (
        <>
          Are you sure you want to delete <strong>{item.returnNumber ?? `return #${item.id}`}</strong>?
          This action cannot be undone.
        </>
      ),
      confirmLabel: 'Delete',
    })
    if (confirmed && item.id) {
      await run(deleteReturn(item.id), { successMessage: 'Return deleted.' })
    }
  }

  const columns: Column<ReturnDto>[] = [
    {
      key: 'reference',
      header: 'Reference',
      render: (row) => (
        <span className="min-w-0">
          <span className="block truncate font-medium text-ink-900">
            {row.returnNumber ?? `#${row.id}`}
          </span>
          <span className="block truncate text-xs text-ink-500">{formatDate(row.returnDate)}</span>
        </span>
      ),
    },
    {
      key: 'type',
      header: 'Type',
      render: (row) => <StatusBadge value={row.type} />,
    },
    {
      key: 'product',
      header: 'Product',
      render: (row) => <span className="text-ink-600">{row.productName ?? '—'}</span>,
    },
    {
      key: 'warehouse',
      header: 'Warehouse',
      render: (row) => <span className="text-ink-600">{row.warehouseName ?? '—'}</span>,
    },
    {
      key: 'quantity',
      header: 'Qty',
      align: 'right',
      render: (row) => <span className="text-ink-700">{row.quantity ?? 0}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge value={row.status} />,
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
              onClick={() => void handleApprove(row)}
              disabled={pending}
            >
              Approve
            </Button>
          )}
          <Button size="icon" variant="ghost" aria-label="Edit return" onClick={() => openEdit(row)}>
            <PencilIcon />
          </Button>
          <Button
            size="icon"
            variant="ghost"
            className="text-red-600 hover:bg-red-50"
            aria-label="Delete return"
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
        title="Returns"
        description="Handle purchase returns to suppliers and customer sales returns."
        actions={
          <Button icon={<PlusIcon />} onClick={openCreate}>
            New return
          </Button>
        }
      />

      <Toolbar end={<span>{filtered.length} returns</span>}>
        <SearchBar value={query} onChange={setQuery} placeholder="Search reference or product…" />
        <div className="w-48">
          <SelectField value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
            <option value="">All types</option>
            <option value="PURCHASE_RETURN">Purchase return</option>
            <option value="SALE_RETURN">Sale return</option>
          </SelectField>
        </div>
      </Toolbar>

      <DataTable
        columns={columns}
        rows={paged}
        rowKey={(row) => row.id!}
        loading={loading}
        skeleton={<TableSkeleton columns={columns.length} />}
        caption="Returns list"
        emptyState={
          error ? (
            <ErrorState message={error} onRetry={() => dispatch(fetchReturns())} />
          ) : filtered.length === 0 && (query || typeFilter) ? (
            <EmptyState
              title="No matches"
              description="No return matches the current filters."
              icon={<UndoIcon />}
            />
          ) : (
            <EmptyState
              title="No returns yet"
              description="Record a return when stock comes back or goes back to a supplier."
              icon={<UndoIcon />}
              action={
                <Button size="sm" icon={<PlusIcon />} onClick={openCreate}>
                  New return
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
        title={editing ? 'Edit return' : 'New return'}
        description={
          editing
            ? 'Only pending returns can be edited.'
            : 'Record the product, quantity and reason for the return.'
        }
        footer={
          <>
            <Button variant="secondary" onClick={closeModal} disabled={pending}>
              Cancel
            </Button>
            <Button type="submit" form="return-form" loading={pending}>
              {editing ? 'Save changes' : 'Create return'}
            </Button>
          </>
        }
      >
        <form id="return-form" onSubmit={handleSubmit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField
            label="Return type"
            required
            value={form.type}
            onChange={(event) => setForm({ ...form, type: event.target.value })}
            hint="Purchase returns reduce stock; sale returns increase it."
          >
            <option value="PURCHASE_RETURN">Purchase return (to supplier)</option>
            <option value="SALE_RETURN">Sale return (from customer)</option>
          </SelectField>

          <SelectField
            label="Product"
            required
            error={errors.productId}
            value={form.productId ?? ''}
            onChange={(event) =>
              setForm({ ...form, productId: event.target.value ? Number(event.target.value) : undefined })
            }
          >
            <option value="">Select product…</option>
            {products.map((product) => (
              <option key={product.id} value={product.id}>
                {product.name} ({product.sku})
              </option>
            ))}
          </SelectField>

          <SelectField
            label="Warehouse"
            required
            error={errors.warehouseId}
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
            label="Quantity"
            type="number"
            step="1"
            required
            error={errors.quantity}
            value={form.quantity ?? ''}
            onChange={(event) =>
              setForm({ ...form, quantity: event.target.value === '' ? undefined : Number(event.target.value) })
            }
          />

          <TextField
            label="Unit price"
            type="number"
            step="0.01"
            value={form.unitPrice ?? ''}
            onChange={(event) =>
              setForm({ ...form, unitPrice: event.target.value === '' ? undefined : Number(event.target.value) })
            }
            hint="Used to calculate the total when left blank."
          />

          <TextField
            label="Total amount"
            type="number"
            step="0.01"
            value={form.totalAmount ?? ''}
            onChange={(event) =>
              setForm({ ...form, totalAmount: event.target.value === '' ? undefined : Number(event.target.value) })
            }
          />

          <TextField
            label="Batch number"
            value={form.batchNumber}
            onChange={(event) => setForm({ ...form, batchNumber: event.target.value })}
            placeholder="Optional"
          />

          <TextAreaField
            label="Reason"
            value={form.reason}
            onChange={(event) => setForm({ ...form, reason: event.target.value })}
            placeholder="e.g. Damaged in transit"
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
