import { Link } from 'react-router-dom'
import { Button } from '../components/ui'

/** Fallback route for unknown URLs. */
export default function NotFound() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center text-center">
      <p className="text-sm font-semibold text-brand-600">404</p>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight text-ink-900">Page not found</h1>
      <p className="mt-2 max-w-md text-sm text-ink-500">
        The page you are looking for doesn&apos;t exist or may have been moved.
      </p>
      <Link to="/dashboard" className="mt-6">
        <Button>Back to dashboard</Button>
      </Link>
    </div>
  )
}
