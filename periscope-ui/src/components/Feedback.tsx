import type { ReactNode } from 'react';

export function Loading({ label = 'Carregando…' }: { label?: string }) {
  return (
    <div className="loading-block" role="status" aria-live="polite">
      {label}
    </div>
  );
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="empty-block">{children}</div>;
}

export function ErrorAlert({ message }: { message: string }) {
  return (
    <div className="alert alert-error" role="alert">
      {message}
    </div>
  );
}

export function SuccessAlert({ message }: { message: string }) {
  return (
    <div className="alert alert-success" role="status">
      {message}
    </div>
  );
}
