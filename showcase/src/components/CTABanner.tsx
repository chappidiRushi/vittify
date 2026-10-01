import { useInView } from '../hooks/useInView'
import { motion } from 'framer-motion'
import { Github, Download } from 'lucide-react'

export function CTABanner() {
  const { ref, inView } = useInView(0.15)

  return (
    <section className="py-20 px-6">
      <motion.div
        ref={ref as React.RefObject<HTMLDivElement>}
        initial={{ opacity: 0, y: 32 }}
        animate={inView ? { opacity: 1, y: 0 } : {}}
        transition={{ duration: 0.7, ease: [0.25, 0.46, 0.45, 0.94] }}
        className="max-w-4xl mx-auto relative overflow-hidden rounded-[32px] p-12 text-center"
        style={{
          background:
            'linear-gradient(135deg, rgba(255,107,107,0.12) 0%, rgba(167,139,250,0.10) 50%, rgba(45,212,191,0.08) 100%)',
          border: '1px solid rgba(255,255,255,0.08)',
        }}
      >
        {/* Background glow blobs */}
        <div className="absolute top-0 left-1/4 w-64 h-64 rounded-full bg-[var(--color-accent-coral)]/10 blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 right-1/4 w-64 h-64 rounded-full bg-[var(--color-accent-lavender)]/10 blur-3xl pointer-events-none" />

        <div className="relative">
          {/* Eyebrow */}
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/5 border border-white/8 text-xs text-[var(--color-text-secondary)] mb-6">
            <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-accent-mint)] animate-pulse" />
            Now in beta — free & open source
          </div>

          {/* Headline */}
          <h2 className="text-4xl sm:text-5xl font-extrabold text-[var(--color-text-primary)] tracking-tighter leading-tight mb-4">
            Ready to track smarter?
          </h2>
          <p className="text-[var(--color-text-secondary)] text-lg mb-10 max-w-lg mx-auto leading-relaxed">
            Build from source in minutes. No account, no cloud, no subscription — ever.
          </p>

          {/* CTAs */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <a
              href="https://github.com/chappidiRushi/vittify"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center gap-2.5 px-8 py-4 text-sm font-bold bg-[var(--color-text-primary)] text-[var(--color-bg)] rounded-2xl hover:opacity-92 transition-opacity shadow-lg"
            >
              <Github size={18} />
              Get the Source
            </a>
            <a
              href="https://github.com/chappidiRushi/vittify/releases"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center gap-2.5 px-8 py-4 text-sm font-semibold text-[var(--color-text-primary)] rounded-2xl border border-white/12 hover:bg-white/5 transition-all"
            >
              <Download size={18} />
              Download APK
            </a>
          </div>

          {/* Sub-note */}
          <p className="mt-6 text-xs text-[var(--color-text-tertiary)]">
            Android 8.0+ · No root required · AGPL-3.0 open source
          </p>
        </div>
      </motion.div>
    </section>
  )
}
