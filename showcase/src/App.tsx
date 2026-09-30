import { useEffect, useState } from 'react'
import { Header } from './components/Header'
import { Hero } from './components/Hero'
import { TrustBar } from './components/TrustBar'
import { Features } from './components/Features'
import { Screenshots } from './components/Screenshots'
import { HowItWorks } from './components/HowItWorks'
import { BankTicker } from './components/BankTicker'
import { PrivacyPledge } from './components/PrivacyPledge'
import { TechStack } from './components/TechStack'
import { FAQ } from './components/FAQ'
import { OpenSource } from './components/OpenSource'
import { CTABanner } from './components/CTABanner'
import { Footer } from './components/Footer'

function ScrollProgress() {
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    const onScroll = () => {
      const total = document.documentElement.scrollHeight - window.innerHeight
      setProgress(total > 0 ? (window.scrollY / total) * 100 : 0)
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <div
      className="fixed top-0 left-0 h-[2px] z-[100] transition-all duration-75"
      style={{
        width: `${progress}%`,
        background:
          'linear-gradient(90deg, var(--color-accent-coral), var(--color-accent-lavender))',
      }}
      role="progressbar"
      aria-valuenow={Math.round(progress)}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label="Page scroll progress"
    />
  )
}

export default function App() {
  return (
    <div className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text-primary)]">
      <ScrollProgress />
      <Header />
      <main>
        <Hero />
        <TrustBar />
        <Features />
        <Screenshots />
        <HowItWorks />
        <BankTicker />
        <PrivacyPledge />
        <TechStack />
        <FAQ />
        <OpenSource />
        <CTABanner />
      </main>
      <Footer />
    </div>
  )
}
