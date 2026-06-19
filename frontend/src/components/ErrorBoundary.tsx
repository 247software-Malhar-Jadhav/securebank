import React from "react";

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/**
 * ErrorBoundary — catches render-time exceptions anywhere below it so a single
 * broken component doesn't blank the whole app. React only supports this via a
 * class component (there is no hook equivalent for componentDidCatch).
 *
 * The fallback UI is intentionally framework-agnostic (no i18n hooks here, since
 * the i18n context itself might be what failed); strings come from props.
 */
export class ErrorBoundary extends React.Component<
  { children: React.ReactNode; fallback: (error: Error) => React.ReactNode },
  ErrorBoundaryState
> {
  state: ErrorBoundaryState = { hasError: false, error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    // In a real deployment this is where we'd ship to Sentry/observability.
    console.error("Uncaught render error:", error, info.componentStack);
  }

  render() {
    if (this.state.hasError && this.state.error) {
      return this.props.fallback(this.state.error);
    }
    return this.props.children;
  }
}
