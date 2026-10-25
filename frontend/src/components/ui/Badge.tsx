import type { ReactNode } from 'react'

export type BadgeTone = 'neutral' | 'brand' | 'success' | 'warning' | 'danger' | 'info'

const tones: Record<BadgeTone, string> = {
  neutral: 'bg-ink-100 text-ink-700 ring-ink-200',
  brand: 'bg-brand-50 text-brand-700 ring-brand-200',
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  warning: 'bg-amber-50 text-amber-700 ring-amber-200',
  danger: 'bg-red-50 text-red-700 ring-red-200',
  info: 'bg-sky-50 text-sky-700 ring-sky-200',
}

/** Maps a domain status/type string to a consistent visual tone. */
export function toneForStatus(value?: string): BadgeTone {
  switch ((value ?? '').toUpperCase()) {
    case 'COMPLETED':
    case 'RECEIVED':
    case 'APPROVED':
    case 'ACTIVE':
    case 'IN':
    case 'PURCHASE':
    case 'SUCCEEDED':
      return 'success'
    case 'PENDING':
    case 'DRAFT':
    case 'EXPIRY_WARNING':
    case 'ENQUEUED':
      return 'warning'
    case 'CANCELLED':
    case 'REJECTED':
    case 'EXPIRY_ALERT':
    case 'OUT':
    case 'LOW_STOCK':
    case 'DISABLED':
    case 'FAILED':
      return 'danger'
    case 'RETURNED':
    case 'SALE':
    case 'SALE_RETURN':
    case 'RUNNING':
      return 'info'
    case 'PURCHASE_RETURN':
    case 'ADJUSTMENT':
    case 'TRANSFER_IN':
    case 'TRANSFER_OUT':
      return 'brand'
    default:
      return 'neutral'
  }
}

interface BadgeProps {
  children: ReactNode
  tone?: BadgeTone
  className?: string
}

/** Compact, non-interactive status pill. */
export function Badge({ children, tone = 'neutral', className = '' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${tones[tone]} ${className}`}
    >
      {children}
    </span>
  )
}

/** Convenience wrapper that derives the tone from a status string. */
export function StatusBadge({ value, className }: { value?: string; className?: string }) {
  if (!value) return <span className="text-ink-400">—</span>
  return (
    <Badge tone={toneForStatus(value)} className={className}>
      {value.replace(/_/g, ' ')}
    </Badge>
  )
}
