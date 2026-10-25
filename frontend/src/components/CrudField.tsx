import { CheckboxField, SelectField, TextAreaField, TextField } from './ui'

/** A single editable field rendered inside a CRUD modal. */
export interface CrudField {
  name: string
  label: string
  type?: 'text' | 'number' | 'email' | 'url' | 'date' | 'textarea' | 'select' | 'checkbox'
  required?: boolean
  placeholder?: string
  hint?: string
  step?: string
  options?: Array<{ value: string | number; label: string }>
  /** Grid columns (out of 2) this field spans; defaults to 1. */
  colSpan?: 1 | 2
  /** Per-field validator returning an error message. */
  validate?: (value: unknown, form: Record<string, unknown>) => string | undefined
}

interface FieldRendererProps {
  field: CrudField
  value: unknown
  error?: string
  onChange: (value: unknown) => void
}

/** Renders the correct form control for a {@link CrudField} definition. */
export function FieldRenderer({ field, value, error, onChange }: FieldRendererProps) {
  const wrapperClass = field.colSpan === 2 || field.type === 'textarea' ? 'sm:col-span-2' : ''
  const common = {
    label: field.label,
    required: field.required,
    error,
    hint: field.hint,
  }

  const content = (() => {
    switch (field.type) {
      case 'textarea':
        return (
          <TextAreaField
            {...common}
            value={(value as string) ?? ''}
            onChange={(event) => onChange(event.target.value)}
            placeholder={field.placeholder}
          />
        )
      case 'select':
        return (
          <SelectField
            {...common}
            value={(value as string | number) ?? ''}
            onChange={(event) => onChange(event.target.value)}
          >
            <option value="">Select…</option>
            {field.options?.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </SelectField>
        )
      case 'checkbox':
        return (
          <div className="flex h-full items-center pt-6">
            <CheckboxField
              label={field.label}
              description={field.hint}
              checked={Boolean(value)}
              onChange={(event) => onChange(event.target.checked)}
            />
          </div>
        )
      case 'number':
        return (
          <TextField
            {...common}
            type="number"
            step={field.step ?? '0.01'}
            value={(value as number | string) ?? ''}
            onChange={(event) => onChange(event.target.value === '' ? undefined : Number(event.target.value))}
            placeholder={field.placeholder}
          />
        )
      case 'date':
        return (
          <TextField
            {...common}
            type="date"
            value={(value as string) ?? ''}
            onChange={(event) => onChange(event.target.value || undefined)}
          />
        )
      default:
        return (
          <TextField
            {...common}
            type={field.type ?? 'text'}
            value={(value as string) ?? ''}
            onChange={(event) => onChange(event.target.value)}
            placeholder={field.placeholder}
          />
        )
    }
  })()

  return <div className={wrapperClass}>{content}</div>
}
