import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react'

const controlClass =
  'w-full rounded-lg border border-ink-300 bg-white px-3 py-2 text-sm text-ink-900 shadow-sm transition placeholder:text-ink-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30 focus:outline-none disabled:cursor-not-allowed disabled:bg-ink-50'

function FieldShell({
  label,
  htmlFor,
  error,
  hint,
  required,
  children,
}: {
  label?: string
  htmlFor: string
  error?: string
  hint?: string
  required?: boolean
  children: ReactNode
}) {
  return (
    <div className="space-y-1">
      {label && (
        <label htmlFor={htmlFor} className="block text-sm font-medium text-ink-700">
          {label}
          {required && <span className="ml-0.5 text-red-500">*</span>}
        </label>
      )}
      {children}
      {error ? (
        <p id={`${htmlFor}-error`} role="alert" className="text-xs text-red-600">
          {error}
        </p>
      ) : hint ? (
        <p className="text-xs text-ink-500">{hint}</p>
      ) : null}
    </div>
  )
}

export interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
}

/** Labelled text input with error/hint messaging and ARIA wiring. */
export function TextField({ label, error, hint, id, className = '', ...rest }: TextFieldProps) {
  const fieldId = id ?? rest.name ?? label?.toLowerCase().replace(/\s+/g, '-')
  return (
    <FieldShell label={label} htmlFor={fieldId!} error={error} hint={hint} required={rest.required}>
      <input
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${fieldId}-error` : undefined}
        className={`${controlClass} ${error ? 'border-red-400 focus:border-red-500 focus:ring-red-500/30' : ''} ${className}`}
        {...rest}
      />
    </FieldShell>
  )
}

export interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  hint?: string
  children: ReactNode
}

/** Labelled native select — keeps keyboard and mobile behaviour for free. */
export function SelectField({ label, error, hint, id, className = '', children, ...rest }: SelectFieldProps) {
  const fieldId = id ?? rest.name ?? label?.toLowerCase().replace(/\s+/g, '-')
  return (
    <FieldShell label={label} htmlFor={fieldId!} error={error} hint={hint} required={rest.required}>
      <select
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${fieldId}-error` : undefined}
        className={`${controlClass} ${error ? 'border-red-400' : ''} ${className}`}
        {...rest}
      >
        {children}
      </select>
    </FieldShell>
  )
}

export interface TextAreaFieldProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
  hint?: string
}

/** Labelled multi-line input. */
export function TextAreaField({ label, error, hint, id, className = '', ...rest }: TextAreaFieldProps) {
  const fieldId = id ?? rest.name ?? label?.toLowerCase().replace(/\s+/g, '-')
  return (
    <FieldShell label={label} htmlFor={fieldId!} error={error} hint={hint} required={rest.required}>
      <textarea
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${fieldId}-error` : undefined}
        className={`${controlClass} min-h-20 resize-y ${error ? 'border-red-400' : ''} ${className}`}
        {...rest}
      />
    </FieldShell>
  )
}

export interface CheckboxFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string
  description?: string
}

/** Labelled checkbox used for booleans such as `active` and `trackBatch`. */
export function CheckboxField({ label, description, id, className = '', ...rest }: CheckboxFieldProps) {
  const fieldId = id ?? rest.name ?? label.toLowerCase().replace(/\s+/g, '-')
  return (
    <div className="flex items-start gap-2.5">
      <input
        id={fieldId}
        type="checkbox"
        className={`mt-0.5 size-4 shrink-0 rounded border-ink-300 text-brand-600 focus:ring-brand-500 ${className}`}
        {...rest}
      />
      <div className="leading-tight">
        <label htmlFor={fieldId} className="text-sm font-medium text-ink-700">
          {label}
        </label>
        {description && <p className="text-xs text-ink-500">{description}</p>}
      </div>
    </div>
  )
}
