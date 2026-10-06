import { useEffect } from 'react'
import { motion } from 'framer-motion'
import { 
  Scale, 
  ArrowLeft, 
  ShieldCheck, 
  Mail, 
  ExternalLink,
  ChevronRight,
  ArrowUp,
  AlertCircle
} from 'lucide-react'
import { termsOfServiceData } from '../data/legal'

interface TermsOfServicePageProps {
  onNavigate: (route: 'home' | 'privacy' | 'terms') => void
}

export function TermsOfServicePage({ onNavigate }: TermsOfServicePageProps) {
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' })
    document.title = 'Terms of Service — Vittify'
  }, [])

  return (
    <div className="pt-24 pb-20 min-h-screen">
      {/* Background radial lavender glow */}
      <div className="fixed top-20 left-1/2 -translate-x-1/2 w-[800px] h-[350px] rounded-full bg-[var(--color-accent-lavender)]/5 blur-[120px] pointer-events-none -z-10" />

      <div className="max-w-4xl mx-auto px-6">
        {/* Navigation & Breadcrumb Bar */}
        <div className="flex flex-wrap items-center justify-between gap-4 mb-8">
          <button
            onClick={() => onNavigate('home')}
            className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] bg-[var(--color-surface)] border border-[var(--color-border)] hover:border-white/10 transition-all cursor-pointer"
          >
            <ArrowLeft size={14} />
            Back to Overview
          </button>

          {/* Tab switcher between Privacy & Terms */}
          <div className="inline-flex items-center p-1 rounded-full bg-[var(--color-surface)] border border-[var(--color-border)] text-xs">
            <button
              onClick={() => onNavigate('privacy')}
              className="px-3.5 py-1 rounded-full font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors cursor-pointer"
            >
              Privacy Policy
            </button>
            <button
              onClick={() => onNavigate('terms')}
              className="px-3.5 py-1 rounded-full font-medium bg-[var(--color-accent-lavender)]/15 text-[var(--color-accent-lavender)] cursor-pointer"
            >
              Terms of Service
            </button>
          </div>
        </div>

        {/* Hero Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-12"
        >
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-[var(--color-accent-lavender)]/10 text-[var(--color-accent-lavender)] mb-4">
            <Scale size={13} />
            User Agreement & Terms
          </div>

          <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight text-[var(--color-text-primary)] mb-4">
            {termsOfServiceData.title}
          </h1>

          <div className="flex flex-wrap items-center gap-3 text-xs text-[var(--color-text-tertiary)] mb-6">
            <span className="px-2.5 py-0.5 rounded-md bg-[var(--color-surface-high)] font-medium text-[var(--color-text-secondary)]">
              Effective: {termsOfServiceData.effectiveDate}
            </span>
            <span>•</span>
            <span>License: AGPL-3.0</span>
            <span>•</span>
            <span>Scope: Mobile App & Showcase Website</span>
          </div>

          {/* Terms Overview Platter */}
          <div className="p-6 sm:p-8 rounded-3xl bg-[var(--color-surface)] border border-[var(--color-border)] relative overflow-hidden">
            <div className="absolute top-0 right-0 w-48 h-48 bg-[var(--color-accent-lavender)]/5 rounded-full blur-2xl pointer-events-none" />

            <p className="text-xs font-bold tracking-wider uppercase text-[var(--color-accent-lavender)] mb-2">
              Summary of Terms
            </p>
            <p className="text-base sm:text-lg text-[var(--color-text-primary)] font-medium leading-relaxed mb-6">
              "{termsOfServiceData.manifesto}"
            </p>

            {/* Feature Pills */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              {termsOfServiceData.summaryPills.map((pill) => (
                <div
                  key={pill.label}
                  className="px-3 py-2 rounded-xl bg-[var(--color-surface-high)] border border-[var(--color-border)] text-[11px] font-medium text-[var(--color-text-secondary)] flex items-center gap-2"
                >
                  <span
                    className="w-1.5 h-1.5 rounded-full shrink-0"
                    style={{ backgroundColor: pill.accent }}
                  />
                  <span>{pill.label}</span>
                </div>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Quick Table of Contents */}
        <div className="mb-12 p-5 rounded-2xl bg-[var(--color-surface)]/60 border border-[var(--color-border)]">
          <p className="text-xs font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider mb-3">
            Table of Contents
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
            {termsOfServiceData.sections.map((section) => (
              <a
                key={section.id}
                href={`#${section.id}`}
                className="flex items-center gap-1.5 text-[var(--color-text-secondary)] hover:text-[var(--color-accent-lavender)] transition-colors py-1 truncate"
              >
                <ChevronRight size={12} className="shrink-0 text-[var(--color-text-tertiary)]" />
                <span className="truncate">{section.title}</span>
              </a>
            ))}
          </div>
        </div>

        {/* Terms Sections */}
        <div className="space-y-6">
          {termsOfServiceData.sections.map((section, idx) => (
            <motion.section
              key={section.id}
              id={section.id}
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-40px' }}
              transition={{ duration: 0.4, delay: idx * 0.03 }}
              className="p-6 sm:p-8 rounded-3xl bg-[var(--color-surface)] border border-[var(--color-border)] scroll-mt-24"
            >
              <div className="flex items-start justify-between gap-4 mb-1">
                <h2 className="text-lg sm:text-xl font-bold text-[var(--color-text-primary)]">
                  {section.title}
                </h2>
                {section.id === 'nature-and-disclaimer' && (
                  <span className="shrink-0 flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full bg-[var(--color-accent-amber)]/10 text-[var(--color-accent-amber)]">
                    <AlertCircle size={12} />
                    Important Disclaimer
                  </span>
                )}
              </div>

              {section.subtitle && (
                <p className="text-xs font-medium text-[var(--color-text-secondary)] mb-5">
                  {section.subtitle}
                </p>
              )}

              <hr className="border-[var(--color-border)] mb-5 opacity-60" />

              {section.paragraphs && (
                <div className="space-y-3.5 text-sm text-[var(--color-text-secondary)] leading-relaxed">
                  {section.paragraphs.map((p, pIdx) => (
                    <p key={pIdx}>{p}</p>
                  ))}
                </div>
              )}
            </motion.section>
          ))}
        </div>

        {/* Contact & Inquiries Platter */}
        <div className="mt-12 p-6 sm:p-8 rounded-3xl bg-[var(--color-surface-high)] border border-[var(--color-border)] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Mail size={16} className="text-[var(--color-accent-lavender)]" />
              <h3 className="text-sm font-bold text-[var(--color-text-primary)]">
                Questions regarding terms or licensing?
              </h3>
            </div>
            <p className="text-xs text-[var(--color-text-secondary)] leading-relaxed max-w-lg">
              For licensing inquiries or clarification on our terms, contact the maintainers at{' '}
              <a
                href="mailto:thegodscode@gmail.com"
                className="text-[var(--color-text-primary)] underline hover:text-[var(--color-accent-lavender)] transition-colors"
              >
                thegodscode@gmail.com
              </a>{' '}
              or visit our GitHub repository.
            </p>
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            <a
              href="https://github.com/chappidiRushi/vittify"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-full text-xs font-medium bg-[var(--color-surface)] border border-[var(--color-border)] hover:border-white/10 text-[var(--color-text-primary)] transition-colors w-full sm:w-auto"
            >
              <ExternalLink size={13} />
              GitHub Repo
            </a>
            <button
              onClick={() => onNavigate('privacy')}
              className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-full text-xs font-medium bg-[var(--color-accent-lavender)] text-black hover:opacity-90 transition-opacity w-full sm:w-auto cursor-pointer"
            >
              <ShieldCheck size={13} />
              View Privacy Policy
            </button>
          </div>
        </div>

        {/* Bottom return to top */}
        <div className="mt-8 flex justify-center">
          <button
            onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
            className="flex items-center gap-1.5 text-xs text-[var(--color-text-tertiary)] hover:text-[var(--color-text-secondary)] transition-colors cursor-pointer"
          >
            <ArrowUp size={12} />
            Back to top of Terms
          </button>
        </div>
      </div>
    </div>
  )
}
