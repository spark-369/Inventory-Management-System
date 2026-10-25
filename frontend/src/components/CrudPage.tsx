import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { useAppDispatch, useConfirm, useMutation, useTableData } from '../hooks'
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
  TableSkeleton,
  Toolbar,
  type Column,
  type SortState,
} from './ui'
import { PencilIcon, PlusIcon, TrashIcon } from './ui/icons'
import { FieldRenderer, type CrudField } from './CrudField'

/** A single editable field rendered inside the create/edit modal. */
export type { CrudField }

export interface CrudPageProps<TRow, TForm extends Record<string, unknown>> {
  title: string
  description: string
  /** Singular noun used in button labels, e.g. "category". */
  entityLabel: string
  /** Plural noun used in counters and empty states. */
  entityLabelPlural: string
  icon: ReactNode
  rows: TRow[]
  loading: boolean
  error: string | null
  /** Thunk creator invoked on mount and when the user retries, e.g. `() => fetchCategories()`. */
  onRefresh: () => unknown
  columns: Column<TRow>[]
  searchable: (row: TRow) => Array<string | number | null | undefined>
  sortValues: Record<string, (row: TRow) => string | number | null | undefined>
  fields: CrudField[]
  /** Initial/blank form state. */
  emptyForm: TForm
  /** Maps a table row into editable form values. */
  toForm: (row: TRow) => TForm
  /** Maps form values into the API payload. */
  toPayload: (form: TForm) => unknown
  onCreate?: (payload: never) => unknown
  onUpdate?: (args: { id: number; data: never }) => unknown
  onDelete?: (id: number) => unknown
  /** Optional extra content above the table (filters, etc.). */
  toolbarExtra?: ReactNode
  /** Optional per-row action buttons rendered before edit/delete. */
  rowActions?: (row: TRow) => ReactNode
  emptyDescription?: string
  modalSize?: 'sm' | 'md' | 'lg' | 'xl'
  canEdit?: boolean
  canCreate?: boolean
  /** Set false for append-only resources (e.g. stock movements). */
  canDelete?: boolean
  /** Extra text appended to the delete confirmation. */
  deleteWarning?: string
}

function labelFor(entityLabel: string): string {
  return entityLabel.charAt(0).toUpperCase() + entityLabel.slice(1)
}

/**
 * Generic, production-ready list/CRUD screen: search, sortable columns,
 * pagination, create/edit modal with validation, delete confirmation and toasts.
 * Resource pages supply configuration only — no duplicated scaffolding.
 */
export function CrudPage<TRow, TForm extends Record<string, unknown>>({
  title,
  description,
  entityLabel,
  entityLabelPlural,
  icon,
  rows,
  loading,
  error,
  onRefresh,
  columns,
  searchable,
  sortValues,
  fields,
  emptyForm,
  toForm,
  toPayload,
  onCreate,
  onUpdate,
  onDelete,
  toolbarExtra,
  rowActions,
  emptyDescription,
  modalSize = 'lg',
  canEdit = true,
  canCreate = true,
  canDelete = true,
  deleteWarning,
}: CrudPageProps<TRow, TForm>) {
  const dispatch = useAppDispatch()
  const { run, pending } = useMutation()
  const { confirm, confirmState, handleConfirm, handleCancel } = useConfirm()

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editing, setEditing] = useState<TRow | null>(null)
  const [form, setForm] = useState<TForm>(emptyForm)
  const [errors, setErrors] = useState<Record<string, string | undefined>>({})

  const refreshRef = useRef(onRefresh)
  refreshRef.current = onRefresh

  const refresh = useCallback(() => {
    // Pages may return a single thunk or an array of thunks (when a screen
    // depends on several lookup lists).
    const result = refreshRef.current()
    if (Array.isArray(result)) {
      result.forEach((item) => dispatch(item as never))
    } else {
      dispatch(result as never)
    }
  }, [dispatch])

  // Load data once on mount; `refresh` is stable so this cannot loop.
  useEffect(() => {
    refresh()
  }, [refresh])

  const search = useMemo(() => searchable, [searchable])
  const table = useTableData<TRow>({ rows, searchable: search, sortValues })

  const openCreate = () => {
    setEditing(null)
    setForm(emptyForm)
    setErrors({})
    setIsModalOpen(true)
  }

  const openEdit = (row: TRow) => {
    setEditing(row)
    setForm(toForm(row))
    setErrors({})
    setIsModalOpen(true)
  }

  const closeModal = () => {
    setIsModalOpen(false)
    setEditing(null)
    setForm(emptyForm)
    setErrors({})
  }

  const setFieldValue = (name: string, value: unknown) => {
    setForm((prev) => ({ ...prev, [name]: value }) as TForm)
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: undefined }))
  }

  const validateAll = (): boolean => {
    const nextErrors: Record<string, string | undefined> = {}
    for (const field of fields) {
      const value = form[field.name]
      if (field.required && (value === '' || value === undefined || value === null)) {
        nextErrors[field.name] = `${field.label} is required.`
        continue
      }
      const customError = field.validate?.(value, form)
      if (customError) nextErrors[field.name] = customError
    }
    setErrors(nextErrors)
    return Object.values(nextErrors).every((message) => !message)
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!validateAll()) return

    const payload = toPayload(form) as never
    const editingId = (editing as { id?: number } | null)?.id

    if (editingId && canEdit) {
      if (!onUpdate) return
      const result = await run(onUpdate({ id: editingId, data: payload }) as never, {
        successMessage: `${labelFor(entityLabel)} updated.`,
      })
      if (result !== undefined) closeModal()
      return
    }

    if (!onCreate) return
    const result = await run(onCreate(payload) as never, {
      successMessage: `${labelFor(entityLabel)} created.`,
    })
    if (result !== undefined) closeModal()
  }

  const handleDelete = async (row: TRow) => {
    const id = (row as { id?: number }).id
    if (!id || !onDelete) return
    const confirmed = await confirm({
      title: `Delete ${entityLabel}`,
      message: (
        <>
          Are you sure you want to delete this {entityLabel}? This action cannot be undone.
          {deleteWarning ? ` ${deleteWarning}` : ''}
        </>
      ),
      confirmLabel: 'Delete',
    })
    if (confirmed) {
      await run(onDelete(id) as never, { successMessage: `${labelFor(entityLabel)} deleted.` })
    }
  }

  const actionColumn: Column<TRow> = {
    key: '__actions',
    header: 'Actions',
    align: 'right',
    render: (row) => (
      <div className="flex items-center justify-end gap-1">
        {rowActions?.(row)}
        {canEdit && (
          <Button
            size="icon"
            variant="ghost"
            aria-label={`Edit ${entityLabel}`}
            onClick={() => openEdit(row)}
          >
            <PencilIcon />
          </Button>
        )}
        {canDelete && (
          <Button
            size="icon"
            variant="ghost"
            className="text-red-600 hover:bg-red-50"
            aria-label={`Delete ${entityLabel}`}
            onClick={() => void handleDelete(row)}
          >
            <TrashIcon />
          </Button>
        )}
      </div>
    ),
  }

  const allColumns = canEdit || canDelete || rowActions ? [...columns, actionColumn] : columns

  return (
    <div className="space-y-6">
      <PageHeader
        title={title}
        description={description}
        actions={
          canCreate ? (
            <Button icon={<PlusIcon />} onClick={openCreate}>
              Add {entityLabel}
            </Button>
          ) : undefined
        }
      />

      <Toolbar
        end={
          <span>
            {table.total} {table.total === 1 ? entityLabel : entityLabelPlural}
          </span>
        }
      >
        <SearchBar
          value={table.query}
          onChange={table.setQuery}
          placeholder={`Search ${entityLabelPlural}…`}
        />
        {toolbarExtra}
      </Toolbar>

      <DataTable
        columns={allColumns}
        rows={table.rows}
        rowKey={(row) => (row as { id?: number }).id ?? JSON.stringify(row)}
        sort={table.sort as SortState | undefined}
        onSortChange={table.setSort}
        loading={loading}
        skeleton={<TableSkeleton columns={allColumns.length} />}
        caption={`${title} list`}
        emptyState={
          error ? (
            <ErrorState message={error} onRetry={refresh} />
          ) : table.isFiltered ? (
            <EmptyState title="No matches" description={`Nothing matches “${table.query}”.`} icon={icon} />
          ) : (
            <EmptyState
              title={`No ${entityLabelPlural} yet`}
              description={emptyDescription ?? `Create your first ${entityLabel} to get started.`}
              icon={icon}
              action={
                canCreate ? (
                  <Button size="sm" icon={<PlusIcon />} onClick={openCreate}>
                    Add {entityLabel}
                  </Button>
                ) : undefined
              }
            />
          )
        }
        footer={
          <Pagination
            page={table.page}
            pageSize={table.pageSize}
            total={table.total}
            onPageChange={table.setPage}
          />
        }
      />

      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editing && canEdit ? `Edit ${entityLabel}` : `Create ${entityLabel}`}
        description={
          editing && canEdit ? `Update the ${entityLabel} details below.` : `Add a new ${entityLabel}.`
        }
        size={modalSize}
        footer={
          <>
            <Button variant="secondary" onClick={closeModal} disabled={pending}>
              Cancel
            </Button>
            <Button type="submit" form="crud-form" loading={pending}>
              {editing && canEdit ? 'Save changes' : `Create ${entityLabel}`}
            </Button>
          </>
        }
      >
        <form id="crud-form" onSubmit={handleSubmit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {fields.map((field) => (
            <FieldRenderer
              key={field.name}
              field={field}
              value={form[field.name]}
              error={errors[field.name]}
              onChange={(value) => setFieldValue(field.name, value)}
            />
          ))}
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
