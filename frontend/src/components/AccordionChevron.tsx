type AccordionChevronProps = {
  expanded: boolean
  className?: string
}

export function AccordionChevron({ expanded, className = '' }: AccordionChevronProps) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 20 20"
      fill="currentColor"
      className={`h-5 w-5 shrink-0 text-slate-500 transition-transform duration-150 ease-out ${
        expanded ? 'rotate-0' : '-rotate-90'
      } ${className}`}
    >
      <path
        fillRule="evenodd"
        d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.168l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06z"
        clipRule="evenodd"
      />
    </svg>
  )
}
