import { useMemo } from 'react'
import { useAppSelector } from '../hooks'
import { CrudPage } from '../components/CrudPage'
import type { CrudField } from '../components/CrudField'
import { createProduct, deleteProduct, fetchProducts, updateProduct } from '../store/productsSlice'
import { fetchCategories } from '../store/categoriesSlice'
import { fetchBrands } from '../store/brandsSlice'
import { fetchSuppliers } from '../store/suppliersSlice'
import { Badge, StatusBadge } from '../components/ui'
import { BoxIcon } from '../components/ui/icons'
import { formatCurrency, formatNumber } from '../utils/format'
import type { ProductDto } from '../types/api'

interface Form extends Record<string, unknown> {
  sku: string
  name: string
  description: string
  categoryId: number | undefined
  brandId: number | undefined
  supplierId: number | undefined
  barcode: string
  imageUrl: string
  purchasePrice: number | undefined
  sellingPrice: number | undefined
  mrp: number | undefined
  reorderLevel: number | undefined
  currentStock: number | undefined
  unit: string
  active: boolean
  trackBatch: boolean
  trackExpiry: boolean
}

const emptyForm: Form = {
  sku: '',
  name: '',
  description: '',
  categoryId: undefined,
  brandId: undefined,
  supplierId: undefined,
  barcode: '',
  imageUrl: '',
  purchasePrice: undefined,
  sellingPrice: undefined,
  mrp: undefined,
  reorderLevel: undefined,
  currentStock: undefined,
  unit: '',
  active: true,
  trackBatch: false,
  trackExpiry: false,
}

export default function ProductsPage() {
  const { items, loading, error } = useAppSelector((state) => state.products)
  const { items: categories } = useAppSelector((state) => state.categories)
  const { items: brands } = useAppSelector((state) => state.brands)
  const { items: suppliers } = useAppSelector((state) => state.suppliers)

  const refresh = () => [fetchProducts(), fetchCategories(), fetchBrands(), fetchSuppliers()]

  const fields: CrudField[] = useMemo(
    () => [
      { name: 'sku', label: 'SKU', required: true, placeholder: 'e.g. BEV-001' },
      { name: 'name', label: 'Product name', required: true, placeholder: 'e.g. Espresso Beans 1kg' },
      {
        name: 'description',
        label: 'Description',
        type: 'textarea',
        placeholder: 'Short description for the catalogue',
      },
      {
        name: 'categoryId',
        label: 'Category',
        type: 'select',
        options: categories.map((c) => ({ value: c.id!, label: c.name })),
      },
      {
        name: 'brandId',
        label: 'Brand',
        type: 'select',
        options: brands.map((b) => ({ value: b.id!, label: b.name })),
      },
      {
        name: 'supplierId',
        label: 'Supplier',
        type: 'select',
        options: suppliers.map((s) => ({ value: s.id!, label: s.name })),
      },
      { name: 'barcode', label: 'Barcode', placeholder: 'Optional EAN/UPC' },
      { name: 'unit', label: 'Unit', placeholder: 'e.g. pcs, kg, box' },
      {
        name: 'purchasePrice',
        label: 'Purchase price',
        type: 'number',
        validate: (value) => (Number(value) < 0 ? 'Purchase price cannot be negative.' : undefined),
      },
      { name: 'sellingPrice', label: 'Selling price', type: 'number' },
      { name: 'mrp', label: 'MRP', type: 'number' },
      { name: 'reorderLevel', label: 'Reorder level', type: 'number', step: '1' },
      {
        name: 'currentStock',
        label: 'Current stock',
        type: 'number',
        step: '1',
        hint: 'Adjusting this directly overrides stock on hand.',
      },
      { name: 'imageUrl', label: 'Image URL', type: 'url', placeholder: 'https://example.com/product.png' },
      { name: 'active', label: 'Active', type: 'checkbox', hint: 'Inactive products stay hidden from sales.' },
      { name: 'trackBatch', label: 'Track batch numbers', type: 'checkbox' },
      { name: 'trackExpiry', label: 'Track expiry dates', type: 'checkbox' },
    ],
    [categories, brands, suppliers],
  )

  return (
    <CrudPage<ProductDto, Form>
      title="Products"
      description="Manage SKUs, pricing, stock levels and reorder thresholds."
      entityLabel="product"
      entityLabelPlural="products"
      icon={<BoxIcon />}
      rows={items}
      loading={loading}
      error={error}
      onRefresh={refresh}
      fields={fields}
      emptyForm={emptyForm}
      modalSize="xl"
      emptyDescription="Add your first product to start tracking stock and pricing."
      toForm={(row) => ({
        sku: row.sku ?? '',
        name: row.name ?? '',
        description: row.description ?? '',
        categoryId: row.categoryId,
        brandId: row.brandId,
        supplierId: row.supplierId,
        barcode: row.barcode ?? '',
        imageUrl: row.imageUrl ?? '',
        purchasePrice: row.purchasePrice,
        sellingPrice: row.sellingPrice,
        mrp: row.mrp,
        reorderLevel: row.reorderLevel,
        currentStock: row.currentStock,
        unit: row.unit ?? '',
        active: row.active ?? true,
        trackBatch: row.trackBatch ?? false,
        trackExpiry: row.trackExpiry ?? false,
      })}
      toPayload={(form) => ({
        sku: form.sku.trim(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        categoryId: form.categoryId ?? undefined,
        brandId: form.brandId ?? undefined,
        supplierId: form.supplierId ?? undefined,
        barcode: form.barcode.trim() || undefined,
        imageUrl: form.imageUrl.trim() || undefined,
        purchasePrice: form.purchasePrice ?? undefined,
        sellingPrice: form.sellingPrice ?? undefined,
        mrp: form.mrp ?? undefined,
        reorderLevel: form.reorderLevel ?? undefined,
        currentStock: form.currentStock ?? undefined,
        unit: form.unit.trim() || undefined,
        active: form.active,
        trackBatch: form.trackBatch,
        trackExpiry: form.trackExpiry,
      })}
      onCreate={createProduct}
      onUpdate={updateProduct}
      onDelete={deleteProduct}
      deleteWarning="Stock movements and batches referencing it may be affected."
      searchable={(row) => [row.name, row.sku, row.barcode, row.categoryName, row.brandName, row.supplierName]}
      sortValues={{
        name: (row) => row.name,
        sku: (row) => row.sku,
        currentStock: (row) => row.currentStock,
        sellingPrice: (row) => row.sellingPrice,
      }}
      columns={[
        {
          key: 'name',
          header: 'Product',
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
                  <BoxIcon />
                </span>
              )}
              <span className="min-w-0">
                <span className="block truncate font-medium text-ink-900">{row.name}</span>
                <span className="block truncate text-xs text-ink-500">{row.sku}</span>
              </span>
            </div>
          ),
        },
        {
          key: 'category',
          header: 'Category',
          sortValue: (row) => row.categoryName,
          render: (row) => <span className="text-ink-600">{row.categoryName ?? '—'}</span>,
        },
        {
          key: 'currentStock',
          header: 'Stock',
          align: 'right',
          sortValue: (row) => row.currentStock,
          render: (row) => {
            const stock = row.currentStock ?? 0
            const reorder = row.reorderLevel ?? 0
            const low = row.reorderLevel != null && stock <= reorder
            return (
              <div className="flex flex-col items-end gap-1">
                <span className="font-medium text-ink-900">{formatNumber(stock)}</span>
                {low && <Badge tone="warning">Reorder at {formatNumber(reorder)}</Badge>}
              </div>
            )
          },
        },
        {
          key: 'sellingPrice',
          header: 'Selling price',
          align: 'right',
          sortValue: (row) => row.sellingPrice,
          render: (row) => <span className="text-ink-700">{formatCurrency(row.sellingPrice)}</span>,
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
