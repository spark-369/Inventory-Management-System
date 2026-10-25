import type { ReactNode } from 'react'

type Tone = 'brand' | 'amber' | 'red' | 'emerald' | 'sky' | 'ink'

const tones: Record<Tone, string> = {
  brand: 'bg-brand-50 text-brand-600',
  amber: 'bg-amber-50 text-amber-600',
  red: 'bg-red-50 text-red-600',
  emerald: 'bg-emerald-50 text-emerald-600',
  sky: 'bg-sky-50 text-sky-600',
  ink: 'bg-ink-100 text-ink-600',
}

interface StatCardProps {
  label: string
  value: ReactNode
  hint?: string
  icon?: ReactNode
  tone?: Tone
  loading?: boolean
}

/** Dashboard KPI tile with an icon chip and optional supporting hint. */
export function StatCard({ label, value, hint, icon, tone = 'brand', loading = false }: StatCardProps) {
  return (
    <div className="card p-5">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-ink-500">{label}</p>
          {loading ? (
            <div className="mt-2 h-7 w-16 animate-pulse rounded bg-ink-100" />
          ) : (
            <p className="mt-1 text-2xl font-semibold tracking-tight text-ink-900">{value}</p>
          )}
          {hint && <p className="mt-1 text-xs text-ink-500">{hint}</p>}
        </div>
        {icon && (
          <div className={`flex size-10 shrink-0 items-center justify-center rounded-lg text-xl ${tones[tone]}`}>
            {icon}
          </div>
        )}
      </div>
    </div>
  )
}
