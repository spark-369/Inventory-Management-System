import { useAppSelector } from '../hooks'
import { CrudPage } from '../components/CrudPage'
import type { CrudField } from '../components/CrudField'
import { createWarehouse, deleteWarehouse, fetchWarehouses, updateWarehouse } from '../store/warehousesSlice'
import { StatusBadge } from '../components/ui'
import { WarehouseIcon } from '../components/ui/icons'
import type { WarehouseDto } from '../types/api'

interface Form extends Record<string, unknown> {
  name: string
  code: string
  address: string
  contactPerson: string
  phone: string
  active: boolean
}

const emptyForm: Form = {
  name: '',
  code: '',
  address: '',
  contactPerson: '',
  phone: '',
  active: true,
}

const fields: CrudField[] = [
  { name: 'name', label: 'Name', required: true, placeholder: 'e.g. Central Warehouse' },
  { name: 'code', label: 'Code', placeholder: 'e.g. WH-01' },
  { name: 'address', label: 'Address', type: 'textarea', placeholder: 'Street, city, postal code' },
  { name: 'contactPerson', label: 'Contact person', placeholder: 'e.g. Jane Doe' },
  { name: 'phone', label: 'Phone', placeholder: 'e.g. +1 555 0100' },
  { name: 'active', label: 'Active', type: 'checkbox', hint: 'Inactive locations cannot receive stock.' },
]

export default function WarehousesPage() {
  const { items, loading, error } = useAppSelector((state) => state.warehouses)

  return (
    <CrudPage<WarehouseDto, Form>
      title="Warehouses"
      description="Storage locations that hold stock for purchases, sales and transfers."
      entityLabel="warehouse"
      entityLabelPlural="warehouses"
      icon={<WarehouseIcon />}
      rows={items}
      loading={loading}
      error={error}
      onRefresh={() => fetchWarehouses()}
      fields={fields}
      emptyForm={emptyForm}
      emptyDescription="Create a warehouse before recording purchases or stock movements."
      toForm={(row) => ({
        name: row.name ?? '',
        code: row.code ?? '',
        address: row.address ?? '',
        contactPerson: row.contactPerson ?? '',
        phone: row.phone ?? '',
        active: row.active ?? true,
      })}
      toPayload={(form) => ({
        name: form.name.trim(),
        code: form.code.trim() || undefined,
        address: form.address.trim() || undefined,
        contactPerson: form.contactPerson.trim() || undefined,
        phone: form.phone.trim() || undefined,
        active: form.active,
      })}
      onCreate={createWarehouse}
      onUpdate={updateWarehouse}
      onDelete={deleteWarehouse}
      deleteWarning="Purchases, sales and batches referencing it may be affected."
      searchable={(row) => [row.name, row.code, row.address, row.contactPerson, row.phone]}
      sortValues={{
        name: (row) => row.name,
        code: (row) => row.code,
      }}
      columns={[
        {
          key: 'name',
          header: 'Name',
          sortValue: (row) => row.name,
          render: (row) => (
            <span className="min-w-0">
              <span className="block truncate font-medium text-ink-900">{row.name}</span>
              <span className="block truncate text-xs text-ink-500">{row.code ?? 'No code'}</span>
            </span>
          ),
        },
        {
          key: 'contactPerson',
          header: 'Contact',
          sortValue: (row) => row.contactPerson,
          render: (row) => <span className="text-ink-600">{row.contactPerson || '—'}</span>,
        },
        {
          key: 'phone',
          header: 'Phone',
          render: (row) => <span className="text-ink-600">{row.phone || '—'}</span>,
        },
        {
          key: 'address',
          header: 'Address',
          render: (row) => <span className="text-ink-600">{row.address || '—'}</span>,
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
