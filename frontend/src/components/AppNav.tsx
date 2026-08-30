import { NavLink } from 'react-router-dom'

const NAV_ITEMS: Array<{ to: string; label: string; end?: boolean }> = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/routines', label: 'Routines' },
  { to: '/exercises', label: 'Exercises' },
  { to: '/workout', label: 'Workout' },
  { to: '/history', label: 'History' },
]

export function AppNav() {
  return (
    <nav aria-label="Main navigation" className="border-t border-slate-200 bg-white">
      <div className="mx-auto flex max-w-5xl gap-1 overflow-x-auto px-4">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              [
                'whitespace-nowrap border-b-2 px-3 py-3 text-sm font-medium transition-colors',
                isActive
                  ? 'border-slate-900 text-slate-900'
                  : 'border-transparent text-slate-600 hover:border-slate-300 hover:text-slate-900',
              ].join(' ')
            }
          >
            {item.label}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
