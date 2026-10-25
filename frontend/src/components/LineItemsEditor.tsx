import { Button } from './ui'
import { PlusIcon, TrashIcon } from './ui/icons'
import { formatCurrency, formatNumber } from '../utils/format'
import type { ProductDto } from '../types/api'

export interface LineItem {
  productId: number
  quantity: number
  unitPrice?: number
  batchNumber?: string
}

interface LineItemsEditorProps {
  items: LineItem[]
  products: ProductDto[]
  onChange: (items: LineItem[]) => void
  /** Price field pre-filled from the product when left blank. */
  priceSource?: 'selling' | 'purchase'
  error?: string
}

const inputClass =
  'w-full rounded-lg border border-ink-300 bg-white px-2.5 py-2 text-sm focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30 focus:outline-none'

/**
 * Repeatable product/quantity/price row editor used by purchase and sale forms.
 * Shows a running total so users can sanity-check the order before saving.
 */
export function LineItemsEditor({
  items,
  products,
  onChange,
  priceSource = 'purchase',
  error,
}: LineItemsEditorProps) {
  const update = (index: number, patch: Partial<LineItem>) => {
    onChange(items.map((item, i) => (i === index ? { ...item, ...patch } : item)))
  }

  const add = () => onChange([...items, { productId: 0, quantity: 1, unitPrice: undefined }])
  const remove = (index: number) => onChange(items.filter((_, i) => i !== index))

  const priceFor = (productId: number): number | undefined => {
    const product = products.find((p) => p.id === productId)
    if (!product) return undefined
    return priceSource === 'selling' ? product.sellingPrice : product.purchasePrice
  }

  const total = items.reduce((sum, item) => sum + (item.unitPrice ?? 0) * (item.quantity || 0), 0)

  return (
    <div className="space-y-3 sm:col-span-2">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-ink-700">Line items</span>
        <Button size="sm" variant="secondary" icon={<PlusIcon />} onClick={add}>
          Add item
        </Button>
      </div>

      {items.length === 0 ? (
        <p className="rounded-lg border border-dashed border-ink-300 px-4 py-6 text-center text-sm text-ink-500">
          No items added yet. Add at least one product to this order.
        </p>
      ) : (
        <div className="space-y-2">
          {items.map((item, index) => (
            <LineItemRow
              key={index}
              index={index}
              item={item}
              products={products}
              inputClass={inputClass}
              onUpdate={update}
              onRemove={remove}
              priceFor={priceFor}
            />
          ))}

          <div className="flex items-center justify-between rounded-lg bg-brand-50 px-4 py-2.5">
            <span className="text-sm font-medium text-brand-800">
              {formatNumber(items.length)} item{items.length === 1 ? '' : 's'}
            </span>
            <span className="text-sm font-semibold text-brand-900">Total {formatCurrency(total)}</span>
          </div>
        </div>
      )}

      {error && (
        <p role="alert" className="text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}

function LineItemRow({
  index,
  item,
  products,
  inputClass,
  onUpdate,
  onRemove,
  priceFor,
}: {
  index: number
  item: LineItem
  products: ProductDto[]
  inputClass: string
  onUpdate: (index: number, patch: Partial<LineItem>) => void
  onRemove: (index: number) => void
  priceFor: (productId: number) => number | undefined
}) {
  const lineTotal = (item.unitPrice ?? 0) * (item.quantity || 0)

  return (
    <div className="grid grid-cols-1 gap-2 rounded-lg border border-ink-200 bg-ink-50/60 p-3 sm:grid-cols-12">
      <div className="sm:col-span-5">
        <label className="mb-1 block text-xs font-medium text-ink-600">Product</label>
        <select
          value={item.productId || ''}
          onChange={(event) => {
            const productId = Number(event.target.value)
            onUpdate(index, { productId, unitPrice: item.unitPrice ?? priceFor(productId) })
          }}
          aria-label={`Product for line ${index + 1}`}
          className={inputClass}
        >
          <option value="">Select product…</option>
          {products.map((product) => (
            <option key={product.id} value={product.id}>
              {product.name} ({product.sku})
            </option>
          ))}
        </select>
      </div>

      <div className="sm:col-span-2">
        <label className="mb-1 block text-xs font-medium text-ink-600">Qty</label>
        <input
          type="number"
          min="1"
          value={item.quantity || ''}
          onChange={(event) => onUpdate(index, { quantity: Number(event.target.value) })}
          aria-label={`Quantity for line ${index + 1}`}
          className={inputClass}
        />
      </div>

      <div className="sm:col-span-2">
        <label className="mb-1 block text-xs font-medium text-ink-600">Unit price</label>
        <input
          type="number"
          step="0.01"
          value={item.unitPrice ?? ''}
          onChange={(event) =>
            onUpdate(index, {
              unitPrice: event.target.value === '' ? undefined : Number(event.target.value),
            })
          }
          aria-label={`Unit price for line ${index + 1}`}
          className={inputClass}
        />
      </div>

      <div className="sm:col-span-2">
        <label className="mb-1 block text-xs font-medium text-ink-600">Batch</label>
        <input
          type="text"
          value={item.batchNumber ?? ''}
          onChange={(event) => onUpdate(index, { batchNumber: event.target.value || undefined })}
          placeholder="Optional"
          aria-label={`Batch number for line ${index + 1}`}
          className={inputClass}
        />
      </div>

      <div className="flex items-end justify-end sm:col-span-1">
        <Button
          size="icon"
          variant="ghost"
          className="text-red-600 hover:bg-red-50"
          aria-label={`Remove line ${index + 1}`}
          onClick={() => onRemove(index)}
        >
          <TrashIcon />
        </Button>
      </div>

      <p className="text-right text-xs text-ink-500 sm:col-span-12">
        Line total: <span className="font-medium text-ink-800">{formatCurrency(lineTotal)}</span>
      </p>
    </div>
  )
}
