import { useEffect, useState } from 'react'
import { useRouter } from './hooks/useRouter'
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
import { PrivacyPolicyPage } from './components/PrivacyPolicyPage'
import { TermsOfServicePage } from './components/TermsOfServicePage'

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
  const { route, navigate } = useRouter()

  useEffect(() => {
    if (route === 'home') {
      document.title = 'Vittify — Ready-to-Use Private Expense Tracker & SMS Parser for Android'
    } else if (route === 'privacy') {
      document.title = 'Privacy Policy — Vittify'
    } else if (route === 'terms') {
      document.title = 'Terms of Service — Vittify'
    }
  }, [route])

  return (
    <div className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text-primary)]">
      {/* Accessible skip link */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-[150] focus:px-4 focus:py-2 focus:bg-[var(--color-accent-coral)] focus:text-white focus:font-semibold focus:rounded-xl focus:shadow-2xl focus:outline-none"
      >
        Skip to main content
      </a>
      <ScrollProgress />
      <Header currentRoute={route} onNavigate={navigate} />

      <main id="main-content" tabIndex={-1} className="outline-none">
        {route === 'privacy' && <PrivacyPolicyPage onNavigate={navigate} />}
        {route === 'terms' && <TermsOfServicePage onNavigate={navigate} />}
        {route === 'home' && (
          <>
            <Hero />
            <TrustBar />
            <Features />
            <Screenshots />
            <HowItWorks />
            <BankTicker />
            <PrivacyPledge onNavigate={navigate} />
            <TechStack />
            <FAQ />
            <OpenSource />
            <CTABanner />
          </>
        )}
      </main>

      <Footer onNavigate={navigate} />
    </div>
  )
}
