import { useState } from 'react'
import { Plus, Minus } from 'lucide-react'
import { AnimatePresence, motion } from 'framer-motion'
import { faqs } from '../data/faq'
import { useInView } from '../hooks/useInView'

function FAQItem({ q, a, isOpen, onToggle }: {
  q: string
  a: string
  isOpen: boolean
  onToggle: () => void
}) {
  return (
    <div className="border-b border-[var(--color-border)] last:border-0">
      <button
        className="w-full text-left py-5 flex items-start justify-between gap-4 group"
        onClick={onToggle}
        aria-expanded={isOpen}
      >
        <span
          className={`text-base font-medium leading-snug transition-colors ${
            isOpen ? 'text-[var(--color-text-primary)]' : 'text-[var(--color-text-secondary)] group-hover:text-[var(--color-text-primary)]'
          }`}
        >
          {q}
        </span>
        <span
          className={`flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center transition-all ${
            isOpen
              ? 'bg-[var(--color-accent-coral)]/15 text-[var(--color-accent-coral)]'
              : 'bg-[var(--color-surface-high)] text-[var(--color-text-tertiary)] group-hover:bg-[var(--color-surface-highest)]'
          }`}
        >
          {isOpen ? <Minus size={12} /> : <Plus size={12} />}
        </span>
      </button>

      <AnimatePresence initial={false}>
        {isOpen && (
          <motion.div
            key="answer"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: [0.25, 0.46, 0.45, 0.94] }}
            className="overflow-hidden"
          >
            <p className="pb-5 text-sm text-[var(--color-text-secondary)] leading-relaxed">
              {a}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export function FAQ() {
  const [openIndex, setOpenIndex] = useState<number | null>(0)
  const { ref, inView } = useInView(0.1)

  return (
    <section id="faq" className="py-28">
      <div className="max-w-3xl mx-auto px-6">
        <div className="text-center mb-14">
          <span className="text-xs font-semibold tracking-widest uppercase text-[var(--color-text-tertiary)]">
            FAQ
          </span>
          <h2 className="mt-3 text-4xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
            Questions, answered.
          </h2>
          <p className="mt-3 text-[var(--color-text-secondary)]">
            Everything you need to know before you try it.
          </p>
        </div>

        <motion.div
          ref={ref as React.RefObject<HTMLDivElement>}
          initial={{ opacity: 0, y: 24 }}
          animate={inView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.6 }}
          className="rounded-3xl bg-[var(--color-surface)] border border-[var(--color-border)] px-7"
        >
          {faqs.map((item, i) => (
            <FAQItem
              key={i}
              q={item.q}
              a={item.a}
              isOpen={openIndex === i}
              onToggle={() => setOpenIndex(openIndex === i ? null : i)}
            />
          ))}
        </motion.div>
      </div>
    </section>
  )
}
