import { useInView } from '../hooks/useInView'
import { motion } from 'framer-motion'
import { Server, Database, Wifi, Eye, CloudUpload } from 'lucide-react'

const pillars = [
  {
    icon: Server,
    title: 'Zero Servers',
    description:
      'No backend, no cloud database, no API endpoints. The only compute is your own phone. There is no system to breach.',
    accent: 'var(--color-accent-teal)',
  },
  {
    icon: Database,
    title: 'Encrypted at Rest',
    description:
      'AES-256 Room database with hardware-backed Android Keystore. Add a biometric lock for an extra layer on top.',
    accent: 'var(--color-accent-teal)',
  },
  {
    icon: Wifi,
    title: 'P2P Partner Sync',
    description:
      'Partner sync goes device-to-device over encrypted local mesh (Nearby + WebRTC). No intermediary server touches your data.',
    accent: 'var(--color-accent-teal)',
  },
  {
    icon: CloudUpload,
    title: 'Encrypted Backup',
    description:
      'Google Drive, Nextcloud & WebDAV backups are AES-256 encrypted on-device before upload. The cloud provider sees only ciphertext.',
    accent: 'var(--color-accent-amber)',
  },
  {
    icon: Eye,
    title: 'Fully Auditable',
    description:
      'Every line of code is public on GitHub under AGPL-3.0. You don\'t have to trust us — you can verify.',
    accent: 'var(--color-accent-teal)',
  },
]

export function PrivacyPledge() {
  const { ref, inView } = useInView(0.08)

  return (
    <section id="privacy" className="py-28 relative overflow-hidden">
      {/* Tonal island */}
      <div className="absolute inset-x-4 inset-y-0 sm:inset-x-8 lg:inset-x-12 rounded-[40px] bg-[var(--color-surface)] border border-[var(--color-border)]" />

      {/* Teal glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[300px] rounded-full bg-[var(--color-accent-teal)]/5 blur-[100px] pointer-events-none" />

      <div
        ref={ref as React.RefObject<HTMLDivElement>}
        className="relative max-w-5xl mx-auto px-6"
      >
        {/* Top: editorial headline + body */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-start mb-16">
          {/* Left — headline */}
          <motion.div
            initial={{ opacity: 0, y: 28 }}
            animate={inView ? { opacity: 1, y: 0 } : {}}
            transition={{ duration: 0.7 }}
          >
            <span className="text-xs font-semibold tracking-widest uppercase text-[var(--color-accent-teal)]">
              Privacy by Design
            </span>
            <h2 className="mt-4 text-4xl sm:text-5xl font-extrabold text-[var(--color-text-primary)] tracking-tighter leading-[1.08]">
              Zero servers.
              <br />
              Zero accounts.
              <br />
              <span style={{ color: 'var(--color-accent-teal)' }}>
                Zero compromise.
              </span>
            </h2>
          </motion.div>

          {/* Right — body + quote */}
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={inView ? { opacity: 1, y: 0 } : {}}
            transition={{ duration: 0.6, delay: 0.15 }}
            className="flex flex-col gap-5 pt-2"
          >
            <p className="text-[var(--color-text-secondary)] leading-relaxed">
              Vittify is designed so your financial data never has anywhere to go except
              the encrypted storage on your own device. This isn't a marketing claim — it's
              a structural property of how the app is built.
            </p>
            <p className="text-[var(--color-text-secondary)] leading-relaxed">
              When you optionally back up to Google Drive, Nextcloud, or WebDAV,
              your data is AES-256 encrypted <em>on your device</em> before it uploads.
              The cloud provider sees only opaque ciphertext — never your transactions.
            </p>
            {/* Pull quote */}
            <blockquote className="border-l-2 pl-4 italic text-[var(--color-text-secondary)]" style={{ borderColor: 'var(--color-accent-teal)' }}>
              "Your phone. Your data. Your rules."
            </blockquote>
          </motion.div>
        </div>

        {/* Bottom: 5 pillars */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {pillars.map((pillar, i) => {
            const Icon = pillar.icon
            return (
              <motion.div
                key={pillar.title}
                initial={{ opacity: 0, y: 24 }}
                animate={inView ? { opacity: 1, y: 0 } : {}}
                transition={{ duration: 0.55, delay: 0.25 + i * 0.08 }}
                className="group p-5 rounded-3xl bg-[var(--color-surface-high)] border border-[var(--color-border)] hover:border-white/10 transition-all"
              >
                <div
                  className="w-9 h-9 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform"
                  style={{ backgroundColor: `${pillar.accent}18` }}
                >
                  <Icon size={16} style={{ color: pillar.accent }} />
                </div>
                <h3 className="text-sm font-bold text-[var(--color-text-primary)] mb-2">
                  {pillar.title}
                </h3>
                <p className="text-xs text-[var(--color-text-secondary)] leading-relaxed">
                  {pillar.description}
                </p>
              </motion.div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
