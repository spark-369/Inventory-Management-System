import type { ReactNode, SVGProps } from 'react'

type IconProps = SVGProps<SVGSVGElement>

/**
 * Minimal, dependency-free icon set. Every icon inherits `currentColor` and
 * sizes to the surrounding font so it can be dropped into buttons and nav items.
 */
function Base({ children, ...props }: IconProps & { children: ReactNode }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      width="1em"
      height="1em"
      {...props}
    >
      {children}
    </svg>
  )
}

export const DashboardIcon = (p: IconProps) => (
  <Base {...p}>
    <rect x="3" y="3" width="7" height="9" rx="1.5" />
    <rect x="14" y="3" width="7" height="5" rx="1.5" />
    <rect x="14" y="12" width="7" height="9" rx="1.5" />
    <rect x="3" y="16" width="7" height="5" rx="1.5" />
  </Base>
)

export const TagIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M20.6 13.4 13.4 20.6a2 2 0 0 1-2.8 0l-7.2-7.2A2 2 0 0 1 2.8 12V4.8A2 2 0 0 1 4.8 2.8H12a2 2 0 0 1 1.4.6l7.2 7.2a2 2 0 0 1 0 2.8Z" />
    <circle cx="7.5" cy="7.5" r="1.2" />
  </Base>
)

export const AwardIcon = (p: IconProps) => (
  <Base {...p}>
    <circle cx="12" cy="9" r="5.5" />
    <path d="M8.2 13.6 7 21.5l5-2.7 5 2.7-1.2-7.9" />
  </Base>
)

export const BoxIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M21 8.5v7a2 2 0 0 1-1 1.7l-7 3.9a2 2 0 0 1-2 0l-7-3.9a2 2 0 0 1-1-1.7v-7a2 2 0 0 1 1-1.7l7-3.9a2 2 0 0 1 2 0l7 3.9a2 2 0 0 1 1 1.7Z" />
    <path d="m3.5 7.7 8.5 4.7 8.5-4.7" />
    <path d="M12 12.4V21" />
  </Base>
)

export const CartIcon = (p: IconProps) => (
  <Base {...p}>
    <circle cx="9" cy="20" r="1.4" />
    <circle cx="18" cy="20" r="1.4" />
    <path d="M2.5 3h2.2l2.2 11.2a1.6 1.6 0 0 0 1.6 1.3h8.6a1.6 1.6 0 0 0 1.6-1.3L20.5 7H6" />
  </Base>
)

export const ReceiptIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M6 2.5h12a1 1 0 0 1 1 1v18l-2.5-1.6-2.5 1.6-2.5-1.6L9 21.5l-2.5-1.6L4 21.5v-18a1 1 0 0 1 1-1Z" />
    <path d="M8.5 8h7M8.5 12h7M8.5 16h4" />
  </Base>
)

export const UndoIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M3 8.5h11.5a5.5 5.5 0 1 1 0 11H9" />
    <path d="m6.5 5-3.5 3.5L6.5 12" />
  </Base>
)

export const SwapIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M4 7h13l-3.5-3.5M20 17H7l3.5 3.5" />
  </Base>
)

export const LayersIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="m12 2.8 9 4.7-9 4.7-9-4.7 9-4.7Z" />
    <path d="m3 12.5 9 4.7 9-4.7" />
    <path d="m3 17 9 4.7 9-4.7" />
  </Base>
)

export const BellIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M18 8.5a6 6 0 1 0-12 0c0 6-2 7.5-2 7.5h16s-2-1.5-2-7.5Z" />
    <path d="M10.3 19.5a2 2 0 0 0 3.4 0" />
  </Base>
)

export const WarehouseIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M2.5 21V9.5L12 4.5l9.5 5V21" />
    <path d="M6.5 21v-6.5h11V21" />
    <path d="M6.5 17.5h11" />
  </Base>
)

export const UsersIcon = (p: IconProps) => (
  <Base {...p}>
    <circle cx="9" cy="8" r="3.5" />
    <path d="M2.5 20.5a6.5 6.5 0 0 1 13 0" />
    <path d="M16 5.2a3.5 3.5 0 0 1 0 6.6" />
    <path d="M18 15.2a6.5 6.5 0 0 1 3.5 5.3" />
  </Base>
)

export const TruckIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M2.5 6.5h10v9h-10z" />
    <path d="M12.5 10h4l3.5 3.5v2h-7.5" />
    <circle cx="6.5" cy="18" r="1.6" />
    <circle cx="16.5" cy="18" r="1.6" />
  </Base>
)

export const ShieldIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M12 2.8 20 6v6c0 5-3.4 8.2-8 9.4-4.6-1.2-8-4.4-8-9.4V6l8-3.2Z" />
    <path d="m9 12 2.2 2.2L15.5 10" />
  </Base>
)

export const PlusIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M12 5v14M5 12h14" />
  </Base>
)

export const SearchIcon = (p: IconProps) => (
  <Base {...p}>
    <circle cx="11" cy="11" r="7" />
    <path d="m20 20-3.5-3.5" />
  </Base>
)

export const PencilIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7.5 18.5 3 20l1.5-4.5Z" />
  </Base>
)

export const TrashIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M4 6.5h16M9.5 6.5V4.8a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v1.7" />
    <path d="M6.5 6.5 7.5 20a1.5 1.5 0 0 0 1.5 1.4h6a1.5 1.5 0 0 0 1.5-1.4l1-13.5" />
    <path d="M10.5 10.5v6M13.5 10.5v6" />
  </Base>
)

export const CheckIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="m5 13 4.5 4.5L19 7" />
  </Base>
)

export const CloseIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M6 6l12 12M18 6 6 18" />
  </Base>
)

export const MenuIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M3.5 6.5h17M3.5 12h17M3.5 17.5h17" />
  </Base>
)

export const ChevronLeftIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="m14.5 6-6 6 6 6" />
  </Base>
)

export const ChevronRightIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="m9.5 6 6 6-6 6" />
  </Base>
)

export const ChevronDownIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="m6 9.5 6 6 6-6" />
  </Base>
)

export const SortIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M7 4v16m0 0-3.5-3.5M7 20l3.5-3.5" />
    <path d="M17 20V4m0 0 3.5 3.5M17 4l-3.5 3.5" />
  </Base>
)

export const AlertTriangleIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M10.3 3.9 2.6 17.2A2 2 0 0 0 4.3 20.2h15.4a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
    <path d="M12 9v4.5M12 17h.01" />
  </Base>
)

export const RefreshIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M20.5 11.5a8.5 8.5 0 1 0-2.6 6.2" />
    <path d="M20.5 5v6.5H14" />
  </Base>
)

export const InboxIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M3.5 12.5 6 4.5h12l2.5 8v6a1.5 1.5 0 0 1-1.5 1.5h-14A1.5 1.5 0 0 1 3.5 18.5Z" />
    <path d="M3.5 12.5h5l1 2.5h5l1-2.5h5" />
  </Base>
)

export const TrendUpIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="m3 17 6-6 4 4 8-8" />
    <path d="M15 7h6v6" />
  </Base>
)

export const AlertCircleIcon = (p: IconProps) => (
  <Base {...p}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7.5v5M12 16h.01" />
  </Base>
)

export const ActivityIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M3 12h3.5l2.5-6 3.5 12 2.5-6H21" />
  </Base>
)

export const CpuIcon = (p: IconProps) => (
  <Base {...p}>
    <rect x="6.5" y="6.5" width="11" height="11" rx="2" />
    <path d="M10 3.5v3M14 3.5v3M10 17.5v3M14 17.5v3M3.5 10h3M3.5 14h3M17.5 10h3M17.5 14h3" />
  </Base>
)

export const ClockIcon = (p: IconProps) => (
  <Base {...p}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7v5.5l3.5 2" />
  </Base>
)

export const PlayIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M7 4.8v14.4l12-7.2-12-7.2Z" />
  </Base>
)

export const ExternalLinkIcon = (p: IconProps) => (
  <Base {...p}>
    <path d="M13.5 4.5H20v6.5" />
    <path d="M20 4.5 11 13.5" />
    <path d="M17 14.5V19a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19V8.5A1.5 1.5 0 0 1 5 7h4.5" />
  </Base>
)
