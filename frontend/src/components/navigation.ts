import type { ComponentType, SVGProps } from 'react'
import {
  ActivityIcon,
  AwardIcon,
  BellIcon,
  BoxIcon,
  CartIcon,
  DashboardIcon,
  LayersIcon,
  ReceiptIcon,
  SwapIcon,
  TagIcon,
  TruckIcon,
  UndoIcon,
  UsersIcon,
  WarehouseIcon,
} from '../components/ui/icons'

export interface NavItem {
  to: string
  label: string
  icon: ComponentType<SVGProps<SVGSVGElement>>
  /** Short helper text surfaced in the header and on the dashboard. */
  description: string
  end?: boolean
}

export interface NavSection {
  title: string
  items: NavItem[]
}

/** Single source of truth for sidebar links and per-route metadata. */
export const navSections: NavSection[] = [
  {
    title: 'Overview',
    items: [
      {
        to: '/dashboard',
        label: 'Dashboard',
        icon: DashboardIcon,
        description: 'Stock health, alerts and recent activity at a glance',
        end: true,
      },
    ],
  },
  {
    title: 'Catalogue',
    items: [
      { to: '/products', label: 'Products', icon: BoxIcon, description: 'Manage SKUs, pricing and stock levels' },
      { to: '/categories', label: 'Categories', icon: TagIcon, description: 'Group products into categories' },
      { to: '/brands', label: 'Brands', icon: AwardIcon, description: 'Track manufacturers and brands' },
    ],
  },
  {
    title: 'Operations',
    items: [
      { to: '/purchases', label: 'Purchases', icon: TruckIcon, description: 'Raise and receive supplier orders' },
      { to: '/sales', label: 'Sales', icon: CartIcon, description: 'Create, complete and invoice customer sales' },
      { to: '/returns', label: 'Returns', icon: UndoIcon, description: 'Handle purchase and sales returns' },
    ],
  },
  {
    title: 'Inventory',
    items: [
      { to: '/stock-batches', label: 'Stock Batches', icon: LayersIcon, description: 'Batch numbers, expiry and cost price' },
      { to: '/stock-movements', label: 'Stock Movements', icon: SwapIcon, description: 'Audit trail of every stock change' },
      { to: '/warehouses', label: 'Warehouses', icon: WarehouseIcon, description: 'Storage locations and contacts' },
    ],
  },
  {
    title: 'Administration',
    items: [
      { to: '/alerts', label: 'Alerts', icon: BellIcon, description: 'Low stock and expiry notifications' },
      {
        to: '/background-jobs',
        label: 'Background Jobs',
        icon: ActivityIcon,
        description: 'Message broker health, schedules and message history',
      },
      { to: '/customers', label: 'Customers', icon: UsersIcon, description: 'Customer directory and tax details' },
      { to: '/suppliers', label: 'Suppliers', icon: ReceiptIcon, description: 'Supplier directory and contacts' },
    ],
  },
]

/** Flat lookup used to derive the page title/subtitle from the current path. */
export const navItems: NavItem[] = navSections.flatMap((section) => section.items)

export function findNavItem(pathname: string): NavItem | undefined {
  return navItems.find((item) => (item.end ? pathname === item.to : pathname.startsWith(item.to)))
}
