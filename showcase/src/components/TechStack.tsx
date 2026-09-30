import { useInView } from '../hooks/useInView'
import { motion } from 'framer-motion'

const stack = [
  { name: 'Kotlin 2.3', sub: 'Language', emoji: '🦺' },
  { name: 'Jetpack Compose', sub: 'UI Toolkit', emoji: '🎨' },
  { name: 'Material 3 Expressive', sub: 'Design System', emoji: '✦' },
  { name: 'Hilt 2.58', sub: 'DI Framework', emoji: '⚙️' },
  { name: 'Room 2.8', sub: 'Local Database', emoji: '🗄️' },
  { name: 'WorkManager', sub: 'Background Tasks', emoji: '⏳' },
  { name: 'WebRTC + Nearby', sub: 'P2P Transport', emoji: '📡' },
  { name: 'Gemini AI', sub: 'BYOK Optional', emoji: '✨' },
  { name: 'Haze 1.7', sub: 'Blur Effects', emoji: '🌫️' },
  { name: 'PdfBox Android', sub: 'PDF Parsing', emoji: '📄' },
  { name: 'Ktor Client 3.3', sub: 'HTTP Engine', emoji: '🌐' },
  { name: 'AGPL-3.0', sub: 'Open Source', emoji: '🔓' },
]

export function TechStack() {
  const { ref, inView } = useInView(0.1)

  return (
    <section className="py-24 bg-[var(--color-surface)] relative">
      <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-[var(--color-border)] to-transparent" />
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-[var(--color-border)] to-transparent" />

      <div className="max-w-5xl mx-auto px-6">
        <div className="text-center mb-12">
          <span className="text-xs font-semibold tracking-widest uppercase text-[var(--color-text-tertiary)]">
            Tech Stack
          </span>
          <h2 className="mt-3 text-3xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
            Built with modern Android standards.
          </h2>
        </div>

        <div
          ref={ref as React.RefObject<HTMLDivElement>}
          className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3"
        >
          {stack.map((item, i) => (
            <motion.div
              key={item.name}
              initial={{ opacity: 0, scale: 0.92 }}
              animate={inView ? { opacity: 1, scale: 1 } : {}}
              transition={{ duration: 0.4, delay: i * 0.04 }}
              className="p-4 rounded-2xl bg-[var(--color-surface-high)] border border-[var(--color-border)] hover:border-white/10 hover:bg-[var(--color-surface-highest)] transition-all group"
            >
              <span className="text-2xl">{item.emoji}</span>
              <p className="mt-2 text-sm font-semibold text-[var(--color-text-primary)] leading-tight">
                {item.name}
              </p>
              <p className="mt-0.5 text-xs text-[var(--color-text-tertiary)]">{item.sub}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
