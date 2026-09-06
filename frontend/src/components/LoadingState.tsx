type LoadingStateProps = {
  message?: string
}

export function LoadingState({ message = 'Loading…' }: LoadingStateProps) {
  return (
    <div className="flex min-h-[40vh] items-center justify-center text-slate-600" role="status">
      {message}
    </div>
  )
}
