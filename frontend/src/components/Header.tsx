import { useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAppDispatch, useAppSelector } from '../hooks'
import { fetchAlerts } from '../store/alertsSlice'
import { findNavItem } from './navigation'
import { BellIcon, MenuIcon, RefreshIcon } from './ui/icons'

interface HeaderProps {
  onOpenSidebar: () => void
}

/**
 * Sticky top bar: shows the active section (derived from the route), an
 * unresolved-alert counter and a manual refresh action.
 */
export default function Header({ onOpenSidebar }: HeaderProps) {
  const { pathname } = useLocation()
  const dispatch = useAppDispatch()
  const { items: alerts, loading } = useAppSelector((state) => state.alerts)
  const current = findNavItem(pathname)
  const [refreshed, setRefreshed] = useState(false)

  useEffect(() => {
    dispatch(fetchAlerts())
  }, [dispatch])

  useEffect(() => {
    if (!refreshed) return
    const timeout = window.setTimeout(() => setRefreshed(false), 1200)
    return () => window.clearTimeout(timeout)
  }, [refreshed])

  const handleRefresh = () => {
    dispatch(fetchAlerts())
    setRefreshed(true)
  }

  return (
    <header className="sticky top-0 z-20 border-b border-ink-200 bg-white/90 backdrop-blur">
      <div className="flex items-center gap-3 px-4 py-3 sm:px-6 lg:px-8">
        <button
          type="button"
          onClick={onOpenSidebar}
          aria-label="Open navigation"
          className="rounded-lg p-2 text-ink-600 transition hover:bg-ink-100 lg:hidden"
        >
          <MenuIcon className="text-xl" />
        </button>

        <div className="min-w-0 flex-1">
          <h2 className="truncate text-sm font-semibold text-ink-900">{current?.label ?? 'IMS'}</h2>
          <p className="hidden truncate text-xs text-ink-500 sm:block">
            {current?.description ?? 'Inventory management system'}
          </p>
        </div>

        <button
          type="button"
          onClick={handleRefresh}
          aria-label="Refresh alerts"
          className="rounded-lg p-2 text-ink-500 transition hover:bg-ink-100 hover:text-ink-800"
        >
          <RefreshIcon className={`text-lg ${loading || refreshed ? 'animate-spin' : ''}`} />
        </button>

        <div className="flex items-center gap-2 rounded-lg border border-ink-200 px-3 py-1.5">
          <BellIcon className="text-base text-ink-500" />
          <span className="text-sm font-medium text-ink-700">{alerts.length}</span>
          <span className="hidden text-xs text-ink-500 sm:inline">
            open alert{alerts.length === 1 ? '' : 's'}
          </span>
        </div>
      </div>
    </header>
  )
}
