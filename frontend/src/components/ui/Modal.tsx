import { Dialog, DialogPanel, DialogTitle, Transition, TransitionChild } from '@headlessui/react'
import { Fragment, type ReactNode } from 'react'
import { CloseIcon } from './icons'

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  description?: string
  children: ReactNode
  /** Tailwind max-width class; defaults to `max-w-lg`. */
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl'
  footer?: ReactNode
}

const sizes = {
  sm: 'max-w-sm',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
  '2xl': 'max-w-6xl',
}

/**
 * Accessible modal built on Headless UI's Dialog: focus trap, scroll lock,
 * Escape-to-close and ARIA labelling are handled for us.
 */
export default function Modal({
  isOpen,
  onClose,
  title,
  description,
  children,
  size = 'md',
  footer,
}: ModalProps) {
  return (
    <Transition appear show={isOpen} as={Fragment}>
      <Dialog as="div" className="relative z-50" onClose={onClose}>
        <TransitionChild
          as={Fragment}
          enter="ease-out duration-200"
          enterFrom="opacity-0"
          enterTo="opacity-100"
          leave="ease-in duration-150"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <div className="fixed inset-0 bg-ink-950/50 backdrop-blur-sm" />
        </TransitionChild>

        <div className="fixed inset-0 overflow-y-auto">
          <div className="flex min-h-full items-end justify-center p-0 sm:items-center sm:p-4">
            <TransitionChild
              as={Fragment}
              enter="ease-out duration-200"
              enterFrom="opacity-0 translate-y-4 sm:scale-95"
              enterTo="opacity-100 translate-y-0 sm:scale-100"
              leave="ease-in duration-150"
              leaveFrom="opacity-100 translate-y-0 sm:scale-100"
              leaveTo="opacity-0 translate-y-4 sm:scale-95"
            >
              <DialogPanel
                className={`flex max-h-[92vh] w-full ${sizes[size]} flex-col overflow-hidden rounded-t-2xl bg-white shadow-xl sm:rounded-2xl`}
              >
                <div className="flex items-start justify-between gap-4 border-b border-ink-200 px-6 py-4">
                  <div className="min-w-0">
                    <DialogTitle className="text-base font-semibold text-ink-900">{title}</DialogTitle>
                    {description && <p className="mt-0.5 text-sm text-ink-500">{description}</p>}
                  </div>
                  <button
                    type="button"
                    onClick={onClose}
                    aria-label="Close dialog"
                    className="-mr-1 rounded-lg p-1.5 text-ink-400 transition hover:bg-ink-100 hover:text-ink-700"
                  >
                    <CloseIcon className="text-lg" />
                  </button>
                </div>

                <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>

                {footer && (
                  <div className="flex items-center justify-end gap-3 border-t border-ink-200 bg-ink-50 px-6 py-3">
                    {footer}
                  </div>
                )}
              </DialogPanel>
            </TransitionChild>
          </div>
        </div>
      </Dialog>
    </Transition>
  )
}
