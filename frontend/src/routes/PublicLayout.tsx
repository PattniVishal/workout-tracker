import { Outlet } from 'react-router-dom'

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <main className="mx-auto flex min-h-screen max-w-md items-center px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
