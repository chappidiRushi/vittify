import { useInView } from '../hooks/useInView'
import { motion } from 'framer-motion'
import { MessageSquareText, Cpu, BarChart3 } from 'lucide-react'

const steps = [
  {
    number: '01',
    icon: MessageSquareText,
    title: 'Grant SMS permission.',
    description:
      'A one-time, transparent permission request. Vittify only reads messages from known bank senders — nothing personal, nothing else.',
    color: 'var(--color-accent-coral)',
  },
  {
    number: '02',
    icon: Cpu,
    title: 'Vittify does the work.',
    description:
      '155+ on-device parsers recognize your bank\'s SMS format, extract the amount, merchant, and account, and categorize the transaction automatically — in milliseconds.',
    color: 'var(--color-accent-lavender)',
  },
  {
    number: '03',
    icon: BarChart3,
    title: 'See your financial story.',
    description:
      'A clean timeline, budget progress bars, savings trends, and subscription costs. All computed locally, all immediately readable.',
    color: 'var(--color-accent-mint)',
  },
]

export function HowItWorks() {
  const { ref, inView } = useInView(0.1)

  return (
    <section id="how-it-works" className="py-28">
      <div className="max-w-6xl mx-auto px-6">
        {/* Header */}
        <div className="text-center mb-16">
          <span className="text-xs font-semibold tracking-widest uppercase text-[var(--color-accent-mint)]">
            How It Works
          </span>
          <h2 className="mt-3 text-4xl sm:text-5xl font-extrabold text-[var(--color-text-primary)] tracking-tighter">
            Three steps.
            <br />
            <span className="text-[var(--color-text-secondary)] font-medium">Then it just works.</span>
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
