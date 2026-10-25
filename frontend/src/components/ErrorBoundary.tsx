import { Component, type ErrorInfo, type ReactNode } from 'react'

interface ErrorBoundaryProps {
  children: ReactNode
}

interface ErrorBoundaryState {
  error: Error | null
}

/**
 * Catches render-time errors anywhere in the tree so a single broken page
 * cannot blank the whole application.
 */
export default class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Surface the details in the console for debugging; a real app would ship
    // this to an error-reporting service.
    console.error('Unhandled UI error:', error, info.componentStack)
  }

  handleReset = () => {
    this.setState({ error: null })
  }

  render() {
    const { error } = this.state
    if (!error) return this.props.children

    return (
      <div className="flex min-h-screen items-center justify-center bg-ink-50 p-6">
        <div className="card w-full max-w-lg p-8 text-center">
          <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-red-50 text-xl text-red-600">
            !
          </div>
          <h1 className="mt-4 text-lg font-semibold text-ink-900">Something went wrong</h1>
          <p className="mt-2 text-sm text-ink-600">
            An unexpected error occurred while rendering this page.
          </p>
          <pre className="mt-4 max-h-40 overflow-auto rounded-lg bg-ink-100 p-3 text-left text-xs text-ink-700">
            {error.message}
          </pre>
          <div className="mt-6 flex justify-center gap-3">
            <button
              type="button"
              onClick={this.handleReset}
              className="rounded-lg border border-ink-300 bg-white px-4 py-2 text-sm font-medium text-ink-700 hover:bg-ink-50"
            >
              Try again
            </button>
            <button
              type="button"
              onClick={() => window.location.assign('/dashboard')}
              className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
            >
              Back to dashboard
            </button>
          </div>
        </div>
      </div>
    )
  }
}
