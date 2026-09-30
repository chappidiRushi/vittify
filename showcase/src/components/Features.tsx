import { useInView } from '../hooks/useInView'
import { motion } from 'framer-motion'
import { features } from '../data/features'

const tagColors: Record<string, string> = {
  coral: 'var(--color-accent-coral)',
  lavender: 'var(--color-accent-lavender)',
  mint: 'var(--color-accent-mint)',
  teal: 'var(--color-accent-teal)',
  amber: 'var(--color-accent-amber)',
}

function FeatureBlock({
  feature,
  index,
}: {
  feature: (typeof features)[0]
  index: number
}) {
  const { ref, inView } = useInView(0.12)
  const isEven = index % 2 === 0
  const base = import.meta.env.BASE_URL
  const accentColor = tagColors[feature.tagColor] || tagColors.coral

  return (
    <motion.div
      ref={ref as React.RefObject<HTMLDivElement>}
      initial={{ opacity: 0, y: 40 }}
      animate={inView ? { opacity: 1, y: 0 } : {}}
      transition={{ duration: 0.7, ease: [0.25, 0.46, 0.45, 0.94] }}
      className={`grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-center ${
        !isEven ? 'lg:[&>*:first-child]:order-last' : ''
      }`}
    >
      {/* Screenshot */}
      <div className="flex justify-center">
        <motion.div
          whileHover={{ scale: 1.03, rotate: isEven ? 1 : -1 }}
          transition={{ type: 'spring', stiffness: 300, damping: 20 }}
          className="relative w-56 h-[440px] cursor-default"
        >
          {/* Glow */}
          <div
            className="absolute inset-0 -m-8 rounded-full blur-3xl opacity-20"
            style={{ backgroundColor: accentColor }}
          />
          {/* Phone shell */}
          <div className="relative w-full h-full rounded-[44px] bg-gradient-to-b from-[#2A2A3A] to-[#1A1A28] shadow-2xl border border-white/8 overflow-hidden">
            <div className="absolute inset-[5px] rounded-[39px] bg-[#0B0B14] overflow-hidden">
              {/* Dynamic island */}
              <div className="absolute top-3 left-1/2 -translate-x-1/2 w-20 h-6 bg-black rounded-full z-10" />
              <img
                src={`${base}screenshots/${feature.screenshot}`}
                alt={feature.screenshotAlt}
                className="absolute inset-0 w-full h-full object-cover object-top"
                loading="lazy"
              />
              <div className="absolute inset-0 bg-gradient-to-br from-white/3 via-transparent to-transparent" />
            </div>
          </div>

          {/* Tag badge */}
          <div
            className="absolute -top-3 -right-3 px-3 py-1 text-xs font-semibold rounded-full text-[var(--color-bg)] shadow-lg"
            style={{ backgroundColor: accentColor }}
          >
            {feature.tag}
          </div>
        </motion.div>
      </div>

      {/* Content */}
      <div className="flex flex-col gap-6">
        <div>
          <span
            className="text-xs font-semibold tracking-widest uppercase"
            style={{ color: accentColor }}
          >
            {feature.label}
          </span>
          <h2 className="mt-3 text-3xl sm:text-4xl font-bold leading-tight text-[var(--color-text-primary)] tracking-tight">
            {feature.headline}
          </h2>
        </div>

        <p className="text-base text-[var(--color-text-secondary)] leading-relaxed">
          {feature.description}
        </p>

        <ul className="flex flex-col gap-3">
          {feature.bullets.map((b, i) => (
            <li key={i} className="flex items-start gap-3 text-sm text-[var(--color-text-secondary)]">
              <span
                className="mt-0.5 flex-shrink-0 w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold"
                style={{
                  backgroundColor: `${accentColor}18`,
                  color: accentColor,
                }}
              >
                ✓
              </span>
              {b}
            </li>
          ))}
        </ul>
      </div>
    </motion.div>
  )
}

export function Features() {
  return (
    <section id="features" className="py-28">
      <div className="max-w-6xl mx-auto px-6">
        {/* Section header */}
        <div className="text-center mb-20">
          <span className="text-xs font-semibold tracking-widest uppercase text-[var(--color-accent-coral)]">
            Features
          </span>
          <h2 className="mt-3 text-4xl sm:text-5xl font-extrabold text-[var(--color-text-primary)] tracking-tighter">
            Everything you need.
            <br />
            <span className="text-[var(--color-text-secondary)] font-medium">Nothing you don't.</span>
          </h2>
        </div>

        {/* Feature blocks */}
        <div className="flex flex-col gap-28">
          {features.map((feature, i) => (
            <FeatureBlock key={feature.id} feature={feature} index={i} />
          ))}
        </div>
      </div>
    </section>
  )
}
