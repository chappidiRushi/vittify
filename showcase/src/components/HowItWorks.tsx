import { useInView } from '../hooks/useInView'
import { motion } from 'framer-motion'
import { MessageSquareText, Cpu, BarChart3 } from 'lucide-react'

const steps = [
  {
    number: '01',
    icon: MessageSquareText,
    title: 'Zero-effort setup.',
    description:
      'Grant a one-time SMS permission, or pin the 1-tap Home Screen Quick Add & Scan Receipt widgets. Vittify only inspects verified bank senders — completely offline.',
    color: 'var(--color-accent-coral)',
  },
  {
    number: '02',
    icon: Cpu,
    title: 'Vittify does the heavy lifting.',
    description:
      '157+ on-device parsers and ML Kit receipt OCR extract amounts, merchants, accounts, and itemized splits automatically — in milliseconds, with zero server delay.',
    color: 'var(--color-accent-lavender)',
  },
  {
    number: '03',
    icon: BarChart3,
    title: 'Instant financial clarity.',
    description:
      'Enjoy effortless money tracking: clean scannable timelines, adaptive budget allowances, split line items, and partner sync. Built for daily convenience and peace of mind.',
    color: 'var(--color-accent-mint)',
  },
]

export function HowItWorks() {
  const { ref, inView } = useInView(0.1)

  return (
    <section id="how-it-works" className="py-28" aria-label="How Vittify works">
      <div className="max-w-6xl mx-auto px-6">
        {/* Header */}
        <div className="text-center mb-16">
          <span className="text-xs font-semibold tracking-widest uppercase text-[var(--color-accent-mint)]">
            Built for Convenience
          </span>
          <h2 className="mt-3 text-4xl sm:text-5xl font-extrabold text-[var(--color-text-primary)] tracking-tighter">
            Three simple steps.
            <br />
            <span className="text-[var(--color-text-secondary)] font-medium">Tracking money becomes effortless.</span>
          </h2>
        </div>

        {/* Steps */}
        <div
          ref={ref as React.RefObject<HTMLDivElement>}
          className="grid grid-cols-1 md:grid-cols-3 gap-6 relative"
        >
          {/* Connector line (desktop) */}
          <div className="hidden md:block absolute top-12 left-[calc(33.33%+1.5rem)] right-[calc(33.33%+1.5rem)] h-px bg-gradient-to-r from-[var(--color-accent-coral)]/40 via-[var(--color-accent-lavender)]/40 to-[var(--color-accent-mint)]/40" />

          {steps.map((step, i) => {
            const Icon = step.icon
            return (
              <motion.div
                key={step.number}
                initial={{ opacity: 0, y: 32 }}
                animate={inView ? { opacity: 1, y: 0 } : {}}
                transition={{
                  duration: 0.6,
                  delay: i * 0.15,
                  ease: [0.25, 0.46, 0.45, 0.94],
                }}
                className="relative p-8 rounded-3xl bg-[var(--color-surface)] border border-[var(--color-border)] hover:border-white/10 transition-all group"
              >
                {/* Step number */}
                <div
                  className="absolute -top-4 left-8 w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-[var(--color-bg)] shadow-lg"
                  style={{ backgroundColor: step.color }}
                >
                  {step.number}
                </div>

                <div
                  className="w-12 h-12 rounded-2xl flex items-center justify-center mb-6 mt-2 transition-transform group-hover:scale-110"
                  style={{ backgroundColor: `${step.color}18` }}
                >
                  <Icon size={22} style={{ color: step.color }} />
                </div>

                <h3 className="text-lg font-bold text-[var(--color-text-primary)] mb-3 leading-snug">
                  {step.title}
                </h3>
                <p className="text-sm text-[var(--color-text-secondary)] leading-relaxed">
                  {step.description}
                </p>
              </motion.div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
