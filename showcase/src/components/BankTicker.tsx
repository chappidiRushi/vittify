import { banks } from '../data/banks'

function BankRow({ reverse }: { reverse?: boolean }) {
  const doubled = [...banks, ...banks]

  return (
    <div className="overflow-hidden relative">
      {/* Fade edges */}
      <div className="absolute left-0 top-0 bottom-0 w-24 z-10 bg-gradient-to-r from-[var(--color-bg)] to-transparent" />
      <div className="absolute right-0 top-0 bottom-0 w-24 z-10 bg-gradient-to-l from-[var(--color-bg)] to-transparent" />

      <div className={`flex gap-4 ${reverse ? 'animate-marquee-rtl' : 'animate-marquee-ltr'} w-max`}>
        {doubled.map((bank, i) => (
          <div
            key={`${bank.name}-${i}`}
            className="flex items-center gap-2.5 px-4 py-2.5 rounded-full bg-[var(--color-surface)] border border-[var(--color-border)] whitespace-nowrap flex-shrink-0"
          >
            <span className="text-lg leading-none">{bank.flag}</span>
            <span className="text-sm text-[var(--color-text-secondary)]">{bank.name}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export function BankTicker() {
  return (
    <section
      className="py-20 overflow-hidden"
      aria-label="Supported banks"
    >
      <div className="max-w-6xl mx-auto px-6 mb-10">
        <div className="text-center">
          <span className="text-xs font-semibold tracking-widest uppercase text-[var(--color-accent-amber)]">
            Global Coverage
          </span>
          <h2 className="mt-3 text-3xl sm:text-4xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
            155+ banks.{' '}
            <span className="text-[var(--color-text-secondary)] font-medium">30+ countries.</span>
          </h2>
          <p className="mt-3 text-[var(--color-text-secondary)] max-w-md mx-auto text-sm">
            Vittify's open parser engine supports banks worldwide. If yours isn't listed, contributing a parser takes minutes.
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-4">
        <BankRow />
        <BankRow reverse />
      </div>
    </section>
  )
}
