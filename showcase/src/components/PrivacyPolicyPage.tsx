import { useEffect } from 'react'
import { motion } from 'framer-motion'
import { 
  Shield, 
  ArrowLeft, 
  FileText, 
  Mail, 
  CheckCircle2, 
  XCircle, 
  ExternalLink,
  ChevronRight,
  ArrowUp
} from 'lucide-react'
import { privacyPolicyData } from '../data/legal'

interface PrivacyPolicyPageProps {
  onNavigate: (route: 'home' | 'privacy' | 'terms') => void
}

export function PrivacyPolicyPage({ onNavigate }: PrivacyPolicyPageProps) {
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' })
    document.title = 'Privacy Policy — Vittify'
  }, [])

  return (
    <div className="pt-24 pb-20 min-h-screen">
      {/* Background radial teal glow */}
      <div className="fixed top-20 left-1/2 -translate-x-1/2 w-[800px] h-[350px] rounded-full bg-[var(--color-accent-teal)]/5 blur-[120px] pointer-events-none -z-10" />

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
              className="px-3.5 py-1 rounded-full font-medium bg-[var(--color-accent-teal)]/15 text-[var(--color-accent-teal)] cursor-pointer"
            >
              Privacy Policy
            </button>
            <button
              onClick={() => onNavigate('terms')}
              className="px-3.5 py-1 rounded-full font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors cursor-pointer"
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
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-[var(--color-accent-teal)]/10 text-[var(--color-accent-teal)] mb-4">
            <Shield size={13} />
            Official Legal Document
          </div>

          <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight text-[var(--color-text-primary)] mb-4">
            {privacyPolicyData.title}
          </h1>

          <div className="flex flex-wrap items-center gap-3 text-xs text-[var(--color-text-tertiary)] mb-6">
            <span className="px-2.5 py-0.5 rounded-md bg-[var(--color-surface-high)] font-medium text-[var(--color-text-secondary)]">
              Last Updated: {privacyPolicyData.effectiveDate}
            </span>
            <span>•</span>
            <span>Version 2.0</span>
            <span>•</span>
            <span>Scope: Mobile App & Showcase Website</span>
          </div>

          {/* Privacy Manifesto Platter */}
          <div className="p-6 sm:p-8 rounded-3xl bg-[var(--color-surface)] border border-[var(--color-border)] relative overflow-hidden">
            <div className="absolute top-0 right-0 w-48 h-48 bg-[var(--color-accent-teal)]/5 rounded-full blur-2xl pointer-events-none" />
            
            <p className="text-xs font-bold tracking-wider uppercase text-[var(--color-accent-teal)] mb-2">
              Our Privacy Manifesto
            </p>
            <p className="text-base sm:text-lg text-[var(--color-text-primary)] font-medium leading-relaxed mb-6">
              "{privacyPolicyData.manifesto}"
            </p>

            {/* Feature Pills */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              {privacyPolicyData.summaryPills.map((pill) => (
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
            {privacyPolicyData.sections.map((section) => (
              <a
                key={section.id}
                href={`#${section.id}`}
                className="flex items-center gap-1.5 text-[var(--color-text-secondary)] hover:text-[var(--color-accent-teal)] transition-colors py-1 truncate"
              >
                <ChevronRight size={12} className="shrink-0 text-[var(--color-text-tertiary)]" />
                <span className="truncate">{section.title}</span>
              </a>
            ))}
          </div>
        </div>

        {/* Policy Sections */}
        <div className="space-y-6">
          {privacyPolicyData.sections.map((section, idx) => (
            <motion.section
              key={section.id}
              id={section.id}
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-40px' }}
              transition={{ duration: 0.4, delay: idx * 0.03 }}
              className="p-6 sm:p-8 rounded-3xl bg-[var(--color-surface)] border border-[var(--color-border)] scroll-mt-24"
            >
              <h2 className="text-lg sm:text-xl font-bold text-[var(--color-text-primary)] mb-1">
                {section.title}
              </h2>

              {section.subtitle && (
                <p className="text-xs font-medium text-[var(--color-text-secondary)] mb-5">
                  {section.subtitle}
                </p>
              )}

              <hr className="border-[var(--color-border)] mb-5 opacity-60" />

              {section.points && (
                <ul className="space-y-3">
                  {section.points.map((point, pIdx) => {
                    const isCheck = point.includes('100% locally') || point.includes('strictly opt-in') || point.includes('Export Anytime')
                    const isCross = point.includes('does NOT') || point.includes('zero') || point.includes('filtered out')

                    return (
                      <li
                        key={pIdx}
                        className="flex items-start gap-3 text-sm text-[var(--color-text-secondary)] leading-relaxed"
                      >
                        {isCross ? (
                          <XCircle
                            size={16}
                            className="text-[var(--color-accent-coral)] shrink-0 mt-0.5"
                          />
                        ) : isCheck ? (
                          <CheckCircle2
                            size={16}
                            className="text-[var(--color-accent-mint)] shrink-0 mt-0.5"
                          />
                        ) : (
                          <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-accent-teal)] shrink-0 mt-2" />
                        )}
                        <span>{point}</span>
                      </li>
                    )
                  })}
                </ul>
              )}

              {section.paragraphs && (
                <div className="space-y-3 text-sm text-[var(--color-text-secondary)] leading-relaxed">
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
              <Mail size={16} className="text-[var(--color-accent-teal)]" />
              <h3 className="text-sm font-bold text-[var(--color-text-primary)]">
                Have questions or feedback regarding privacy?
              </h3>
            </div>
            <p className="text-xs text-[var(--color-text-secondary)] leading-relaxed max-w-lg">
              We welcome independent audits and security reports. Contact the maintainer at{' '}
              <a
                href="mailto:thegodscode@gmail.com"
                className="text-[var(--color-text-primary)] underline hover:text-[var(--color-accent-teal)] transition-colors"
              >
                thegodscode@gmail.com
              </a>{' '}
              or inspect the code on GitHub.
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
              onClick={() => onNavigate('terms')}
              className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-full text-xs font-medium bg-[var(--color-accent-teal)] text-black hover:opacity-90 transition-opacity w-full sm:w-auto cursor-pointer"
            >
              <FileText size={13} />
              View Terms
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
            Back to top of Privacy Policy
          </button>
        </div>
      </div>
    </div>
  )
}
