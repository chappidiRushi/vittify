import { useInView } from '../hooks/useInView'
import { motion } from 'framer-motion'
import { ShieldCheck, Globe, Sparkles, Lock } from 'lucide-react'

const stats = [
  {
    icon: ShieldCheck,
    value: '100%',
    label: 'On-Device Parsing',
    detail: 'No cloud. No uploads.',
    color: 'var(--color-accent-mint)',
  },
  {
    icon: Globe,
    value: '155+',
    label: 'Banks Supported',
    detail: '30+ countries worldwide',
    color: 'var(--color-accent-amber)',
  },
  {
    icon: Sparkles,
    value: 'M3',
    label: 'Expressive Design',
    detail: 'Material 3 Expressive UI',
    color: 'var(--color-accent-lavender)',
  },
  {
    icon: Lock,
    value: 'AGPL',
    label: 'Open Source',
    detail: 'Fully auditable code',
    color: 'var(--color-accent-teal)',
  },
]

export function TrustBar() {
  const { ref, inView } = useInView(0.15)

  return (
    <section className="py-16 relative" aria-label="Key features overview">
      {/* Subtle top divider */}
      <div className="absolute top-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-[var(--color-border)] to-transparent" />

      <div className="max-w-6xl mx-auto px-6">
        <div
          ref={ref as React.RefObject<HTMLDivElement>}
          className="grid grid-cols-2 lg:grid-cols-4 gap-4"
        >
          {stats.map((stat, i) => {
            const Icon = stat.icon
            return (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 20 }}
                animate={inView ? { opacity: 1, y: 0 } : {}}
                transition={{ duration: 0.5, delay: i * 0.08 }}
                className="flex flex-col gap-3 p-5 rounded-2xl bg-[var(--color-surface)] border border-[var(--color-border)] hover:border-white/10 transition-all"
              >
                <div
                  className="w-9 h-9 rounded-xl flex items-center justify-center"
                  style={{ backgroundColor: `${stat.color}18` }}
                >
                  <Icon size={16} style={{ color: stat.color }} />
                </div>
                <div>
                  <p
                    className="text-2xl font-extrabold tracking-tight"
                    style={{ color: stat.color }}
                  >
                    {stat.value}
                  </p>
                  <p className="text-sm font-semibold text-[var(--color-text-primary)] mt-0.5">
                    {stat.label}
                  </p>
                  <p className="text-xs text-[var(--color-text-tertiary)] mt-0.5">
                    {stat.detail}
                  </p>
                </div>
              </motion.div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
