import { useAppSelector } from '../hooks'
import { CrudPage } from '../components/CrudPage'
import type { CrudField } from '../components/CrudField'
import { createBrand, deleteBrand, fetchBrands, updateBrand } from '../store/brandsSlice'
import { AwardIcon } from '../components/ui/icons'
import type { BrandDto } from '../types/api'

interface Form extends Record<string, unknown> {
  name: string
  description: string
  logoUrl: string
}

const emptyForm: Form = { name: '', description: '', logoUrl: '' }

const fields: CrudField[] = [
  { name: 'name', label: 'Name', required: true, placeholder: 'e.g. Acme' },
  { name: 'description', label: 'Description', type: 'textarea', placeholder: 'Optional description' },
  {
    name: 'logoUrl',
    label: 'Logo URL',
    type: 'url',
    placeholder: 'https://example.com/logo.png',
  },
]

export default function BrandsPage() {
  const { items, loading, error } = useAppSelector((state) => state.brands)

  return (
    <CrudPage<BrandDto, Form>
      title="Brands"
      description="Track the manufacturers and brands in your catalogue."
      entityLabel="brand"
      entityLabelPlural="brands"
      icon={<AwardIcon />}
      rows={items}
      loading={loading}
      error={error}
      onRefresh={() => fetchBrands()}
      fields={fields}
      emptyForm={emptyForm}
      toForm={(row) => ({
        name: row.name ?? '',
        description: row.description ?? '',
        logoUrl: row.logoUrl ?? '',
      })}
      toPayload={(form) => ({
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        logoUrl: form.logoUrl.trim() || undefined,
      })}
      onCreate={createBrand}
      onUpdate={updateBrand}
      onDelete={deleteBrand}
      deleteWarning="Products assigned to it will be left without a brand."
      searchable={(row) => [row.name, row.description]}
      sortValues={{
        name: (row) => row.name,
        description: (row) => row.description,
      }}
      columns={[
        {
          key: 'name',
          header: 'Name',
          sortValue: (row) => row.name,
          render: (row) => (
            <div className="flex items-center gap-3">
              {row.logoUrl ? (
                <img
                  src={row.logoUrl}
                  alt=""
                  loading="lazy"
                  className="size-9 rounded-lg border border-ink-200 bg-white object-contain p-0.5"
                />
              ) : (
                <span className="flex size-9 items-center justify-center rounded-lg bg-ink-100 text-ink-500">
                  <AwardIcon />
                </span>
              )}
              <span className="font-medium text-ink-900">{row.name}</span>
            </div>
          ),
        },
        {
          key: 'description',
          header: 'Description',
          sortValue: (row) => row.description,
          render: (row) => <span className="text-ink-600">{row.description || '—'}</span>,
        },
      ]}
    />
  )
}
