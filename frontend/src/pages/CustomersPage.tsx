import { useAppSelector } from '../hooks'
import { CrudPage } from '../components/CrudPage'
import type { CrudField } from '../components/CrudField'
import { createCustomer, deleteCustomer, fetchCustomers, updateCustomer } from '../store/customersSlice'
import { StatusBadge } from '../components/ui'
import { UsersIcon } from '../components/ui/icons'
import type { CustomerDto } from '../types/api'

interface Form extends Record<string, unknown> {
  name: string
  email: string
  phone: string
  address: string
  taxId: string
  active: boolean
}

const emptyForm: Form = { name: '', email: '', phone: '', address: '', taxId: '', active: true }

const fields: CrudField[] = [
  { name: 'name', label: 'Name', required: true, placeholder: 'e.g. Acme Retail Ltd' },
  {
    name: 'email',
    label: 'Email',
    type: 'email',
    placeholder: 'billing@example.com',
    validate: (value) =>
      value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value)) ? 'Enter a valid email address.' : undefined,
  },
  { name: 'phone', label: 'Phone', placeholder: 'e.g. +1 555 0100' },
  { name: 'address', label: 'Address', type: 'textarea', placeholder: 'Billing address' },
  { name: 'taxId', label: 'Tax ID', placeholder: 'VAT / TIN number' },
  { name: 'active', label: 'Active', type: 'checkbox', hint: 'Inactive customers cannot be selected on sales.' },
]

export default function CustomersPage() {
  const { items, loading, error } = useAppSelector((state) => state.customers)

  return (
    <CrudPage<CustomerDto, Form>
      title="Customers"
      description="People and businesses you sell to."
      entityLabel="customer"
      entityLabelPlural="customers"
      icon={<UsersIcon />}
      rows={items}
      loading={loading}
      error={error}
      onRefresh={() => fetchCustomers()}
      fields={fields}
      emptyForm={emptyForm}
      toForm={(row) => ({
        name: row.name ?? '',
        email: row.email ?? '',
        phone: row.phone ?? '',
        address: row.address ?? '',
        taxId: row.taxId ?? '',
        active: row.active ?? true,
      })}
      toPayload={(form) => ({
        name: form.name.trim(),
        email: form.email.trim() || undefined,
        phone: form.phone.trim() || undefined,
        address: form.address.trim() || undefined,
        taxId: form.taxId.trim() || undefined,
        active: form.active,
      })}
      onCreate={createCustomer}
      onUpdate={updateCustomer}
      onDelete={deleteCustomer}
      deleteWarning="Sales linked to this customer will lose the association."
      searchable={(row) => [row.name, row.email, row.phone, row.address, row.taxId]}
      sortValues={{
        name: (row) => row.name,
        email: (row) => row.email,
      }}
      columns={[
        {
          key: 'name',
          header: 'Name',
          sortValue: (row) => row.name,
          render: (row) => <span className="font-medium text-ink-900">{row.name}</span>,
        },
        {
          key: 'email',
          header: 'Email',
          sortValue: (row) => row.email,
          render: (row) => <span className="text-ink-600">{row.email || '—'}</span>,
        },
        {
          key: 'phone',
          header: 'Phone',
          render: (row) => <span className="text-ink-600">{row.phone || '—'}</span>,
        },
        {
          key: 'taxId',
          header: 'Tax ID',
          render: (row) => <span className="text-ink-600">{row.taxId || '—'}</span>,
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
