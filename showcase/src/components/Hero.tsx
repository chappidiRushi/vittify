import { useEffect, useState } from 'react'
import { Github, ArrowRight, Shield, Zap, Sparkles } from 'lucide-react'
import { motion, AnimatePresence, useReducedMotion } from 'framer-motion'

const heroScreenshots = [
  'home.png',
  'transactions.png',
  'analytics.png',
  'budgets.png',
  'partner_sync.png',
]

const floatingCards = [
  { emoji: '🛒', merchant: 'Swiggy', amount: '↓ ₹540', sub: 'Food · HDFC', color: 'var(--color-accent-coral)' },
  { emoji: '💼', merchant: 'Salary', amount: '↑ ₹85,000', sub: 'Income · SBI', color: 'var(--color-accent-mint)' },
  { emoji: '☕', merchant: 'Blue Tokai', amount: '↓ ₹420', sub: 'Café · Kotak', color: 'var(--color-accent-coral)' },
  { emoji: '🎬', merchant: 'Netflix', amount: '↓ ₹649', sub: 'Subscription · ICICI', color: 'var(--color-accent-lavender)' },
  { emoji: '🛵', merchant: 'Blinkit', amount: '↓ ₹1,240', sub: 'Groceries · Axis', color: 'var(--color-accent-coral)' },
]

function FloatingCard({ card, visible }: { card: typeof floatingCards[0]; visible: boolean }) {
  return (
    <AnimatePresence mode="wait">
      {visible && (
        <motion.div
          key={card.merchant}
          initial={{ opacity: 0, x: 20, scale: 0.92 }}
          animate={{ opacity: 1, x: 0, scale: 1 }}
          exit={{ opacity: 0, x: -12, scale: 0.94 }}
          transition={{ duration: 0.4, ease: [0.25, 0.46, 0.45, 0.94] }}
          className="flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl border backdrop-blur-sm"
          style={{
            background: 'rgba(18,18,26,0.88)',
            borderColor: 'rgba(255,255,255,0.08)',
            boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
          }}
        >
          <span className="text-xl leading-none">{card.emoji}</span>
          <div className="min-w-0">
            <p className="text-xs font-semibold text-[var(--color-text-primary)] leading-tight truncate">{card.merchant}</p>
            <p className="text-[10px] text-[var(--color-text-tertiary)] leading-tight">{card.sub}</p>
          </div>
          <p className="text-xs font-bold ml-1 flex-shrink-0" style={{ color: card.color }}>
            {card.amount}
          </p>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

function PhoneMockup() {
  const [currentImg, setCurrentImg] = useState(0)
  const [cardIndex, setCardIndex] = useState(0)
  const [cardVisible, setCardVisible] = useState(true)
  const shouldReduceMotion = useReducedMotion()

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentImg((i) => (i + 1) % heroScreenshots.length)
    }, 3200)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    if (shouldReduceMotion) return
    const cycle = setInterval(() => {
      setCardVisible(false)
      setTimeout(() => {
        setCardIndex((i) => (i + 1) % floatingCards.length)
        setCardVisible(true)
      }, 450)
    }, 2800)
    return () => clearInterval(cycle)
  }, [shouldReduceMotion])

  const base = import.meta.env.BASE_URL

  return (
    <div className="relative flex items-center justify-center">
      {/* Glow rings */}
      <div className="absolute w-72 h-72 rounded-full bg-[var(--color-accent-coral)]/5 blur-3xl" />
      <div className="absolute w-48 h-48 rounded-full bg-[var(--color-accent-lavender)]/8 blur-2xl translate-x-16 translate-y-8" />

      {/* Floating transaction card — right side, lg+ only */}
      {!shouldReduceMotion && (
        <div className="hidden lg:block absolute -right-8 top-16 z-20 w-52">
          <FloatingCard card={floatingCards[cardIndex]} visible={cardVisible} />
        </div>
      )}

      {/* Balance badge — bottom left, lg+ only */}
      {!shouldReduceMotion && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.2, duration: 0.5 }}
          className="hidden lg:flex absolute -left-6 bottom-24 z-20 items-center gap-2 px-3.5 py-2.5 rounded-2xl border backdrop-blur-sm"
          style={{
            background: 'rgba(18,18,26,0.88)',
            borderColor: 'rgba(255,255,255,0.08)',
            boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
          }}
        >
          <div className="w-7 h-7 rounded-xl bg-[var(--color-accent-mint)]/15 flex items-center justify-center text-sm">💰</div>
          <div>
            <p className="text-[10px] text-[var(--color-text-tertiary)] leading-none mb-0.5">Net Worth</p>
            <p className="text-xs font-bold text-[var(--color-accent-mint)]">↑ ₹1,24,580</p>
          </div>
        </motion.div>
      )}

      {/* Phone frame */}
      <motion.div
        animate={shouldReduceMotion ? {} : { y: [0, -10, 0] }}
        transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
        className="relative w-56 h-[480px] z-10"
      >
        {/* Outer shell */}
        <div className="absolute inset-0 rounded-[44px] bg-gradient-to-b from-[#2A2A3A] to-[#1A1A28] shadow-2xl border border-white/10" />

        {/* Screen area */}
        <div className="absolute inset-[6px] rounded-[38px] bg-[#0B0B14] overflow-hidden">
          {/* Dynamic island */}
          <div className="absolute top-3 left-1/2 -translate-x-1/2 w-24 h-7 bg-black rounded-full z-10 flex items-center justify-center gap-1.5">
            <div className="w-2 h-2 rounded-full bg-[#1A1A1A]" />
            <div className="w-3 h-3 rounded-full bg-[#1A1A1A]" />
          </div>

          {/* Screenshots */}
          <div className="absolute inset-0">
            {heroScreenshots.map((src, i) => (
              <img
                key={src}
                src={`${base}screenshots/${src}`}
                alt={`Vittify app — ${src.replace('.png', '').replace(/_/g, ' ')} screen`}
                className="absolute inset-0 w-full h-full object-cover object-top transition-opacity duration-700"
                style={{ opacity: i === currentImg ? 1 : 0 }}
                loading={i === 0 ? 'eager' : 'lazy'}
              />
            ))}
          </div>

          {/* Screen reflection */}
          <div className="absolute inset-0 bg-gradient-to-br from-white/4 via-transparent to-transparent pointer-events-none z-20" />
        </div>

        {/* Side buttons */}
        <div className="absolute right-[-3px] top-28 w-[3px] h-10 bg-white/20 rounded-l-sm" />
        <div className="absolute left-[-3px] top-20 w-[3px] h-7 bg-white/20 rounded-r-sm" />
        <div className="absolute left-[-3px] top-32 w-[3px] h-7 bg-white/20 rounded-r-sm" />
        <div className="absolute left-[-3px] top-44 w-[3px] h-7 bg-white/20 rounded-r-sm" />
      </motion.div>

      {/* Screenshot indicator dots */}
      <div className="absolute -bottom-8 flex gap-1.5">
        {heroScreenshots.map((_, i) => (
          <div
            key={i}
            className="h-1 rounded-full transition-all duration-500"
            style={{
              width: i === currentImg ? 20 : 6,
              backgroundColor:
                i === currentImg
                  ? 'var(--color-accent-coral)'
                  : 'var(--color-text-tertiary)',
            }}
          />
        ))}
      </div>
    </div>
  )
}

const badges = [
  { icon: Shield, label: 'On-Device Parsing', color: 'var(--color-accent-mint)' },
  { icon: Zap, label: '155+ Banks', color: 'var(--color-accent-amber)' },
  { icon: Sparkles, label: 'Material 3 Expressive', color: 'var(--color-accent-lavender)' },
]

export function Hero() {
  const shouldReduceMotion = useReducedMotion()

  return (
    <section
      id="hero"
      className="relative min-h-screen flex items-center pt-16 overflow-hidden"
      aria-label="Vittify hero"
    >
      {/* Background radial gradients */}
      <div className="absolute inset-0 pointer-events-none select-none">
        <div className="absolute top-1/4 left-1/4 w-[600px] h-[600px] rounded-full bg-[var(--color-accent-coral)]/4 blur-[120px]" />
        <div className="absolute top-1/3 right-1/4 w-[500px] h-[500px] rounded-full bg-[var(--color-accent-lavender)]/4 blur-[120px]" />
        {/* Grid overlay */}
        <div
          className="absolute inset-0 opacity-[0.02]"
          style={{
            backgroundImage:
              'linear-gradient(var(--color-text-primary) 1px, transparent 1px), linear-gradient(90deg, var(--color-text-primary) 1px, transparent 1px)',
            backgroundSize: '60px 60px',
          }}
        />
      </div>

      <div className="relative max-w-6xl mx-auto px-6 py-24 w-full">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          {/* Left column — copy */}
          <div className="flex flex-col gap-8">
            {/* Eyebrow pill */}
            <motion.div
              initial={shouldReduceMotion ? {} : { opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
            >
              <span className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-[var(--color-accent-coral)] bg-[var(--color-accent-coral)]/10 border border-[var(--color-accent-coral)]/20 rounded-full">
                <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-accent-coral)] animate-pulse" />
                Open Source · AGPL-3.0
              </span>
            </motion.div>

            {/* Headline */}
            <motion.div
              initial={shouldReduceMotion ? {} : { opacity: 0, y: 24 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.7, delay: 0.1 }}
            >
              <h1 className="text-5xl sm:text-6xl font-extrabold leading-[1.08] tracking-tighter text-[var(--color-text-primary)]">
                A tiny, smart
                <br />
                <span className="text-gradient-coral">financial</span>
                <br />
                companion.
              </h1>
            </motion.div>

            {/* Sub-headline */}
            <motion.p
              initial={shouldReduceMotion ? {} : { opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="text-lg text-[var(--color-text-secondary)] leading-relaxed max-w-md"
            >
              Vittify reads your bank SMS on-device, auto-categorizes every transaction,
              and presents your spending as a clean, actionable financial timeline.{' '}
              <strong className="text-[var(--color-text-primary)] font-medium">No cloud. No accounts. No compromise.</strong>
            </motion.p>

            {/* CTAs */}
            <motion.div
              initial={shouldReduceMotion ? {} : { opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.3 }}
              className="flex flex-wrap gap-3"
            >
              <a
                href="https://github.com/chappidiRushi/vittify"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-6 py-3 text-sm font-semibold bg-[var(--color-text-primary)] text-[var(--color-bg)] rounded-full hover:opacity-90 transition-opacity"
              >
                <Github size={16} />
                View on GitHub
              </a>
              <a
                href="#screenshots"
                className="inline-flex items-center gap-2 px-6 py-3 text-sm font-semibold text-[var(--color-text-primary)] bg-[var(--color-surface)] border border-[var(--color-border)] rounded-full hover:bg-[var(--color-surface-high)] hover:border-white/10 transition-all"
              >
                See Screenshots
                <ArrowRight size={14} />
              </a>
            </motion.div>

            {/* Trust badges */}
            <motion.div
              initial={shouldReduceMotion ? {} : { opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 0.6, delay: 0.5 }}
              className="flex flex-wrap gap-3 pt-2"
            >
              {badges.map(({ icon: Icon, label, color }) => (
                <div
                  key={label}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs text-[var(--color-text-secondary)] bg-[var(--color-surface)] border border-[var(--color-border)] rounded-full"
                >
                  <Icon size={12} style={{ color }} />
                  {label}
                </div>
              ))}
            </motion.div>
          </div>

          {/* Right column — phone mockup */}
          <motion.div
            initial={shouldReduceMotion ? {} : { opacity: 0, x: 40 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="flex justify-center lg:justify-end"
          >
            <PhoneMockup />
          </motion.div>
        </div>
      </div>

      {/* Bottom gradient fade */}
      <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-[var(--color-bg)] to-transparent pointer-events-none" />
    </section>
  )
}
