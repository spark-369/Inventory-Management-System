import type { ReactNode } from 'react'

interface ToolbarProps {
  /** Left side: usually a SearchBar plus filter controls. */
  children: ReactNode
  /** Right side: row count, refresh or bulk actions. */
  end?: ReactNode
}

/** Lays out list-page controls above the table. */
export function Toolbar({ children, end }: ToolbarProps) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex flex-wrap items-center gap-2">{children}</div>
      {end && <div className="flex items-center gap-2 text-sm text-ink-500">{end}</div>}
    </div>
  )
}
