import { NavLink } from 'react-router-dom'
import { navSections } from './navigation'
import { CloseIcon, LayersIcon } from './ui/icons'

interface SidebarProps {
  /** Mobile drawer open state (ignored on desktop). */
  open: boolean
  onClose: () => void
}

/**
 * Primary navigation. Fixed on desktop; on mobile it becomes an overlay drawer
 * controlled by the header's menu button.
 */
export default function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      {/* Backdrop for the mobile drawer */}
      <div
        className={`fixed inset-0 z-30 bg-ink-950/50 transition-opacity lg:hidden ${
          open ? 'opacity-100' : 'pointer-events-none opacity-0'
        }`}
        onClick={onClose}
        aria-hidden="true"
      />

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-72 shrink-0 flex-col border-r border-ink-800 bg-ink-950 text-ink-200 transition-transform duration-200 lg:static lg:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
        aria-label="Main navigation"
      >
        <div className="flex items-center justify-between gap-3 border-b border-ink-800 px-5 py-4">
          <div className="flex items-center gap-3">
            <span className="flex size-9 items-center justify-center rounded-lg bg-brand-600 text-lg text-white">
              <LayersIcon />
            </span>
            <span className="leading-tight">
              <span className="block text-sm font-semibold text-white">IMS</span>
              <span className="block text-xs text-ink-400">Inventory Management</span>
            </span>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close navigation"
            className="rounded-lg p-1.5 text-ink-400 transition hover:bg-ink-800 hover:text-white lg:hidden"
          >
            <CloseIcon className="text-lg" />
          </button>
        </div>

        <nav className="flex-1 space-y-6 overflow-y-auto px-3 py-5">
          {navSections.map((section) => (
            <div key={section.title}>
              <p className="px-3 pb-2 text-[11px] font-semibold tracking-wider text-ink-500 uppercase">
                {section.title}
              </p>
              <ul className="space-y-1">
                {section.items.map((item) => (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      end={item.end}
                      onClick={onClose}
                      className={({ isActive }) =>
                        `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                          isActive
                            ? 'bg-brand-600 text-white shadow-sm'
                            : 'text-ink-300 hover:bg-ink-800 hover:text-white'
                        }`
                      }
                    >
                      <item.icon className="shrink-0 text-lg" />
                      <span className="truncate">{item.label}</span>
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>

        <div className="border-t border-ink-800 px-5 py-4">
          <p className="text-xs text-ink-500">IMS v2 · Spring Boot + React</p>
        </div>
      </aside>
    </>
  )
}
