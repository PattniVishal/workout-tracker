import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { ConfirmDialog, type ConfirmDialogProps } from './ConfirmDialog'

type ConfirmOptions = Omit<ConfirmDialogProps, 'isOpen' | 'isPending' | 'onConfirm' | 'onCancel'>

type ConfirmContextValue = {
  confirm: (options: ConfirmOptions) => Promise<boolean>
}

const ConfirmContext = createContext<ConfirmContextValue | null>(null)

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [dialogState, setDialogState] = useState<ConfirmOptions | null>(null)
  const resolveRef = useRef<((value: boolean) => void) | null>(null)

  const confirm = useCallback((options: ConfirmOptions) => {
    return new Promise<boolean>((resolve) => {
      resolveRef.current = resolve
      setDialogState(options)
    })
  }, [])

  const closeDialog = useCallback((result: boolean) => {
    resolveRef.current?.(result)
    resolveRef.current = null
    setDialogState(null)
  }, [])

  const value = useMemo(() => ({ confirm }), [confirm])

  return (
    <ConfirmContext.Provider value={value}>
      {children}
      <ConfirmDialog
        isOpen={dialogState !== null}
        title={dialogState?.title ?? ''}
        message={dialogState?.message ?? ''}
        confirmLabel={dialogState?.confirmLabel}
        cancelLabel={dialogState?.cancelLabel}
        destructive={dialogState?.destructive}
        onConfirm={() => closeDialog(true)}
        onCancel={() => closeDialog(false)}
      />
    </ConfirmContext.Provider>
  )
}

export function useConfirm() {
  const context = useContext(ConfirmContext)

  if (!context) {
    throw new Error('useConfirm must be used within ConfirmProvider')
  }

  return context.confirm
}
