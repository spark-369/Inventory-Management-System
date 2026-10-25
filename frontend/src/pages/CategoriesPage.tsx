import { useAppSelector } from '../hooks'
import { CrudPage } from '../components/CrudPage'
import type { CrudField } from '../components/CrudField'
import { createCategory, deleteCategory, fetchCategories, updateCategory } from '../store/categoriesSlice'
import { TagIcon } from '../components/ui/icons'
import type { CategoryDto } from '../types/api'

interface Form extends Record<string, unknown> {
  name: string
  description: string
  imageUrl: string
}

const emptyForm: Form = { name: '', description: '', imageUrl: '' }

const fields: CrudField[] = [
  { name: 'name', label: 'Name', required: true, placeholder: 'e.g. Beverages' },
  {
    name: 'description',
    label: 'Description',
    type: 'textarea',
    placeholder: 'Optional summary shown in the catalogue',
  },
  {
    name: 'imageUrl',
    label: 'Image URL',
    type: 'url',
    placeholder: 'https://example.com/category.png',
    hint: 'Shown as a thumbnail in the list.',
  },
]

export default function CategoriesPage() {
  const { items, loading, error } = useAppSelector((state) => state.categories)

  return (
    <CrudPage<CategoryDto, Form>
      title="Categories"
      description="Organise your catalogue into logical product groups."
      entityLabel="category"
      entityLabelPlural="categories"
      icon={<TagIcon />}
      rows={items}
      loading={loading}
      error={error}
      onRefresh={() => fetchCategories()}
      fields={fields}
      emptyForm={emptyForm}
      toForm={(row) => ({
        name: row.name ?? '',
        description: row.description ?? '',
        imageUrl: row.imageUrl ?? '',
      })}
      toPayload={(form) => ({
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        imageUrl: form.imageUrl.trim() || undefined,
      })}
      onCreate={createCategory}
      onUpdate={updateCategory}
      onDelete={deleteCategory}
      deleteWarning="Products assigned to it will be left without a category."
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
              {row.imageUrl ? (
                <img
                  src={row.imageUrl}
                  alt=""
                  loading="lazy"
                  className="size-9 rounded-lg border border-ink-200 object-cover"
                />
              ) : (
                <span className="flex size-9 items-center justify-center rounded-lg bg-ink-100 text-ink-500">
                  <TagIcon />
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
