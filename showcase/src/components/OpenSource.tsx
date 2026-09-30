import { useInView } from '../hooks/useInView'
import { motion } from 'framer-motion'
import { Github, GitFork, Star } from 'lucide-react'

export function OpenSource() {
  const { ref, inView } = useInView(0.1)

  return (
    <section className="py-28 relative overflow-hidden">
      {/* Gradient background glow */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[400px] rounded-full bg-[var(--color-accent-coral)]/5 blur-[100px]" />
      </div>

      <div
        ref={ref as React.RefObject<HTMLDivElement>}
        className="relative max-w-4xl mx-auto px-6 text-center"
      >
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={inView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.6 }}
        >
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-3xl bg-[var(--color-surface)] border border-[var(--color-border)] mb-6">
            <Github size={28} className="text-[var(--color-text-secondary)]" />
          </div>

          <span className="block text-xs font-semibold tracking-widest uppercase text-[var(--color-text-tertiary)]">
            Open Source
          </span>

          <h2 className="mt-3 text-4xl sm:text-5xl font-extrabold text-[var(--color-text-primary)] tracking-tighter">
            Free. Forever.
            <br />
            <span className="text-[var(--color-text-secondary)] font-medium">Yours to inspect, fork, extend.</span>
          </h2>

          <p className="mt-5 text-[var(--color-text-secondary)] leading-relaxed max-w-xl mx-auto">
            Vittify is published under the{' '}
            <strong className="text-[var(--color-text-primary)] font-medium">AGPL-3.0 license</strong>.
            Every line of code is public. No paywalls, no premium tiers.
            Forked from the excellent{' '}
            <a
              href="https://github.com/ritesh-kanwar/Cashiro"
              target="_blank"
              rel="noopener noreferrer"
              className="underline underline-offset-2 hover:text-[var(--color-text-primary)] transition-colors"
            >
              Cashiro
            </a>{' '}
            by ritesh-kanwar.
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={inView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="mt-10 flex flex-col sm:flex-row gap-4 justify-center"
        >
          <a
            href="https://github.com/chappidiRushi/Vittify"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center gap-2.5 px-7 py-3.5 text-sm font-semibold bg-[var(--color-text-primary)] text-[var(--color-bg)] rounded-full hover:opacity-90 transition-opacity"
          >
            <Github size={16} />
            View Repository
          </a>
          <a
            href="https://github.com/chappidiRushi/Vittify/stargazers"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center gap-2.5 px-7 py-3.5 text-sm font-semibold text-[var(--color-text-primary)] bg-[var(--color-surface)] border border-[var(--color-border)] rounded-full hover:bg-[var(--color-surface-high)] hover:border-white/10 transition-all"
          >
            <Star size={16} />
            Star on GitHub
          </a>
          <a
            href="https://github.com/chappidiRushi/Vittify/fork"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center gap-2.5 px-7 py-3.5 text-sm font-semibold text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors"
          >
            <GitFork size={16} />
            Fork & Contribute
          </a>
        </motion.div>

        {/* Contribution nudge */}
        <motion.p
          initial={{ opacity: 0 }}
          animate={inView ? { opacity: 1 } : {}}
          transition={{ duration: 0.6, delay: 0.4 }}
          className="mt-8 text-xs text-[var(--color-text-tertiary)]"
        >
          Adding a bank parser is a few dozen lines of Kotlin.{' '}
          <a
            href="https://github.com/chappidiRushi/Vittify/blob/main/CONTRIBUTING.md"
            target="_blank"
            rel="noopener noreferrer"
            className="underline underline-offset-2 hover:text-[var(--color-text-secondary)] transition-colors"
          >
            Read the contributing guide →
          </a>
        </motion.p>
      </div>
    </section>
  )
}
