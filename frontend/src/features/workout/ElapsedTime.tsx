import { useEffect, useState } from 'react'
import { formatDateTime } from '../../lib/formatDateTime'

type ElapsedTimeProps = {
  startedAt: string
}

function formatElapsed(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  if (hours > 0) {
    return `${hours}h ${minutes}m ${seconds}s`
  }

  return `${minutes}m ${seconds}s`
}

export function ElapsedTime({ startedAt }: ElapsedTimeProps) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0)

  useEffect(() => {
    const started = new Date(startedAt).getTime()

    function updateElapsed() {
      const nextElapsed = Math.max(0, Math.floor((Date.now() - started) / 1000))
      setElapsedSeconds(nextElapsed)
    }

    updateElapsed()
    const intervalId = window.setInterval(updateElapsed, 1000)

    return () => {
      window.clearInterval(intervalId)
    }
  }, [startedAt])

  return (
    <p className="text-sm text-slate-600">
      Started {formatDateTime(startedAt)} · Elapsed {formatElapsed(elapsedSeconds)}
    </p>
  )
}
