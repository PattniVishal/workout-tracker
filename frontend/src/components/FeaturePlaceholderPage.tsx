type FeaturePlaceholderPageProps = {
  title: string
  description: string
}

export function FeaturePlaceholderPage({ title, description }: FeaturePlaceholderPageProps) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-semibold">{title}</h2>
      <p className="mt-2 text-slate-600">{description}</p>
      <p className="mt-4 text-sm text-slate-500">This feature is not implemented yet.</p>
    </section>
  )
}
