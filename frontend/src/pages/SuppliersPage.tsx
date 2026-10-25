import { useAppSelector } from '../hooks'
import { CrudPage } from '../components/CrudPage'
import type { CrudField } from '../components/CrudField'
import { createSupplier, deleteSupplier, fetchSuppliers, updateSupplier } from '../store/suppliersSlice'
import { StatusBadge } from '../components/ui'
import { TruckIcon } from '../components/ui/icons'
import type { SupplierDto } from '../types/api'

interface Form extends Record<string, unknown> {
  name: string
  contactPerson: string
  email: string
  phone: string
  address: string
  taxId: string
  active: boolean
}

const emptyForm: Form = {
  name: '',
  contactPerson: '',
  email: '',
  phone: '',
  address: '',
  taxId: '',
  active: true,
}

const fields: CrudField[] = [
  { name: 'name', label: 'Company name', required: true, placeholder: 'e.g. Global Supplies Inc' },
  { name: 'contactPerson', label: 'Contact person', placeholder: 'e.g. John Smith' },
  {
    name: 'email',
    label: 'Email',
    type: 'email',
    placeholder: 'orders@example.com',
    validate: (value) =>
      value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value)) ? 'Enter a valid email address.' : undefined,
  },
  { name: 'phone', label: 'Phone', placeholder: 'e.g. +1 555 0100' },
  { name: 'address', label: 'Address', type: 'textarea', placeholder: 'Street, city, postal code' },
  { name: 'taxId', label: 'Tax ID', placeholder: 'VAT / TIN number' },
  { name: 'active', label: 'Active', type: 'checkbox', hint: 'Inactive suppliers cannot be selected on purchases.' },
]

export default function SuppliersPage() {
  const { items, loading, error } = useAppSelector((state) => state.suppliers)

  return (
    <CrudPage<SupplierDto, Form>
      title="Suppliers"
      description="Vendors you raise purchase orders with."
      entityLabel="supplier"
      entityLabelPlural="suppliers"
      icon={<TruckIcon />}
      rows={items}
      loading={loading}
      error={error}
      onRefresh={() => fetchSuppliers()}
      fields={fields}
      emptyForm={emptyForm}
      toForm={(row) => ({
        name: row.name ?? '',
        contactPerson: row.contactPerson ?? '',
        email: row.email ?? '',
        phone: row.phone ?? '',
        address: row.address ?? '',
        taxId: row.taxId ?? '',
        active: row.active ?? true,
      })}
      toPayload={(form) => ({
        name: form.name.trim(),
        contactPerson: form.contactPerson.trim() || undefined,
        email: form.email.trim() || undefined,
        phone: form.phone.trim() || undefined,
        address: form.address.trim() || undefined,
        taxId: form.taxId.trim() || undefined,
        active: form.active,
      })}
      onCreate={createSupplier}
      onUpdate={updateSupplier}
      onDelete={deleteSupplier}
      deleteWarning="Purchases linked to this supplier will lose the association."
      searchable={(row) => [row.name, row.contactPerson, row.email, row.phone, row.taxId]}
      sortValues={{
        name: (row) => row.name,
        contactPerson: (row) => row.contactPerson,
      }}
      columns={[
        {
          key: 'name',
          header: 'Company',
          sortValue: (row) => row.name,
          render: (row) => <span className="font-medium text-ink-900">{row.name}</span>,
        },
        {
          key: 'contactPerson',
          header: 'Contact',
          sortValue: (row) => row.contactPerson,
          render: (row) => <span className="text-ink-600">{row.contactPerson || '—'}</span>,
        },
        {
          key: 'email',
          header: 'Email',
          render: (row) => <span className="text-ink-600">{row.email || '—'}</span>,
        },
        {
          key: 'phone',
          header: 'Phone',
          render: (row) => <span className="text-ink-600">{row.phone || '—'}</span>,
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
