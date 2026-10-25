import { Badge, Button, SelectField, TextField } from './ui'
import { ClockIcon, PlayIcon } from './ui/icons'
import type { JobMessageTypeOption, JobScheduleDto } from '../types/api'

interface DispatchPanelProps {
  types: JobMessageTypeOption[]
  schedules: JobScheduleDto[]
  dispatchType: string
  onTypeChange: (value: string) => void
  productId: string
  onProductIdChange: (value: string) => void
  /** Some message types (e.g. LOW_STOCK_SCAN) must reference a product. */
  requiresProductId: boolean
  pending: boolean
  onDispatch: () => void
}

/**
 * Lets an operator publish a message by hand and lists the schedules the broker
 * runs automatically. Mirrors the create/edit modal pattern used by {@code CrudPage}
 * (labelled controls, primary action disabled while a request is in flight).
 */
export function DispatchPanel({
  types,
  schedules,
  dispatchType,
  onTypeChange,
  productId,
  onProductIdChange,
  requiresProductId,
  pending,
  onDispatch,
}: DispatchPanelProps) {
  const selected = types.find((type) => type.value === dispatchType)

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <section className="card p-5">
        <h2 className="text-sm font-semibold text-ink-900">Publish a message</h2>
        <p className="mt-1 text-xs text-ink-500">
          Enqueues a message on the broker exactly as the domain services do. The worker picks it up
          asynchronously and the result appears in the history below.
        </p>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
          <SelectField
            label="Message type"
            value={dispatchType}
            onChange={(event) => onTypeChange(event.target.value)}
          >
            <option value="">Select a message…</option>
            {types
              .filter((type) => type.dispatchable !== false)
              .map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
          </SelectField>

          <TextField
            label="Product ID"
            type="number"
            min={1}
            value={productId}
            onChange={(event) => onProductIdChange(event.target.value)}
            disabled={!requiresProductId}
            hint={requiresProductId ? 'Required for this message type.' : 'Not used by the selected message.'}
            placeholder={requiresProductId ? 'e.g. 42' : '—'}
          />
        </div>

        {selected?.description && <p className="mt-3 text-xs text-ink-500">{selected.description}</p>}

        <div className="mt-4">
          <Button
            icon={<PlayIcon />}
            onClick={onDispatch}
            loading={pending}
            disabled={!dispatchType || (requiresProductId && productId.trim() === '')}
          >
            Publish message
          </Button>
        </div>
      </section>

      <section className="card overflow-hidden">
        <div className="border-b border-ink-200 px-5 py-4">
          <h2 className="text-sm font-semibold text-ink-900">Recurring schedules</h2>
          <p className="mt-1 text-xs text-ink-500">
            Registered with JobRunr at startup and persisted, so they survive restarts.
          </p>
        </div>

        {schedules.length === 0 ? (
          <p className="px-5 py-6 text-sm text-ink-500">No recurring schedules are registered.</p>
        ) : (
          <ul className="divide-y divide-ink-100">
            {schedules.map((schedule) => (
              <li key={schedule.id} className="flex flex-wrap items-start gap-3 px-5 py-4">
                <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-base text-brand-600">
                  <ClockIcon />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-ink-900">{schedule.name}</p>
                  <p className="mt-0.5 text-xs text-ink-500">{schedule.description ?? '—'}</p>
                </div>
                <Badge tone="info">{schedule.schedule}</Badge>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
