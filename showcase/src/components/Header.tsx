import { useEffect, useState } from 'react'
import { Github, Menu, X } from 'lucide-react'

interface HeaderProps {
  currentRoute?: 'home' | 'privacy' | 'terms'
  onNavigate?: (route: 'home' | 'privacy' | 'terms', hash?: string) => void
}

const navLinks = [
  { label: 'Features', href: '#features' },
  { label: 'Screenshots', href: '#screenshots' },
  { label: 'How It Works', href: '#how-it-works' },
  { label: 'FAQ', href: '#faq' },
]

export function Header({ currentRoute = 'home', onNavigate }: HeaderProps) {
  const [scrolled, setScrolled] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const handleNavClick = (e: React.MouseEvent<HTMLAnchorElement>, href: string) => {
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return

    e.preventDefault()
    setMenuOpen(false)

    if (href === '/' || href === '#') {
      onNavigate?.('home')
      return
    }

    if (href.startsWith('#')) {
      onNavigate?.('home', href)
      return
    }
  }

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
        scrolled
          ? 'bg-[var(--color-bg)]/90 backdrop-blur-xl border-b border-[var(--color-border)]'
          : 'bg-transparent'
      }`}
    >
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        {/* Logo */}
        <a
          href="/"
          onClick={(e) => handleNavClick(e, '/')}
          className="flex items-center gap-2.5 group cursor-pointer"
          aria-label="Vittify home"
        >
          <div className="w-8 h-8 rounded-xl bg-[var(--color-accent-coral)] flex items-center justify-center text-white font-bold text-sm shadow-lg group-hover:scale-105 transition-transform">
            V
          </div>
          <span className="font-bold text-[var(--color-text-primary)] text-base tracking-tight">
            Vittify
          </span>
        </a>

        {/* Desktop nav */}
        <nav className="hidden md:flex items-center gap-1" aria-label="Primary navigation">
          {navLinks.map((l) => (
            <a
              key={l.href}
              href={currentRoute === 'home' ? l.href : `/${l.href}`}
              onClick={(e) => handleNavClick(e, l.href)}
              className="px-3.5 py-2 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] rounded-lg hover:bg-[var(--color-surface)] transition-all duration-200 cursor-pointer"
            >
              {l.label}
            </a>
          ))}
          <a
            href="/privacy"
            onClick={(e) => {
              if (!e.metaKey && !e.ctrlKey) {
                e.preventDefault()
                onNavigate?.('privacy')
              }
            }}
            className={`px-3.5 py-2 text-sm rounded-lg transition-all duration-200 cursor-pointer ${
              currentRoute === 'privacy'
                ? 'text-[var(--color-accent-teal)] font-medium bg-[var(--color-surface)]'
                : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface)]'
            }`}
          >
            Privacy
          </a>
          <a
            href="/terms"
            onClick={(e) => {
              if (!e.metaKey && !e.ctrlKey) {
                e.preventDefault()
                onNavigate?.('terms')
              }
            }}
            className={`px-3.5 py-2 text-sm rounded-lg transition-all duration-200 cursor-pointer ${
              currentRoute === 'terms'
                ? 'text-[var(--color-accent-lavender)] font-medium bg-[var(--color-surface)]'
                : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface)]'
            }`}
          >
            Terms
          </a>
        </nav>

        {/* Desktop CTA */}
        <div className="hidden md:flex items-center gap-3">
          <a
            href="https://github.com/chappidiRushi/vittify"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 px-3.5 py-2 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] rounded-lg hover:bg-[var(--color-surface)] transition-all duration-200"
            aria-label="View Vittify on GitHub"
          >
            <Github size={16} />
            <span>GitHub</span>
          </a>
          <a
            href="https://github.com/chappidiRushi/vittify/releases"
            target="_blank"
            rel="noopener noreferrer"
            className="px-4 py-2 text-sm font-medium bg-[var(--color-accent-coral)] text-white rounded-full hover:opacity-90 transition-opacity"
          >
            Get App
          </a>
        </div>

        {/* Mobile menu button */}
        <button
          className="md:hidden p-2 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] rounded-lg hover:bg-[var(--color-surface)] transition-all cursor-pointer"
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={menuOpen}
        >
          {menuOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>

      {/* Mobile menu */}
      <div
        className={`md:hidden overflow-hidden transition-all duration-300 ease-in-out ${
          menuOpen ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'
        } bg-[var(--color-bg)]/95 backdrop-blur-xl border-b border-[var(--color-border)]`}
      >
        <nav className="px-6 py-4 flex flex-col gap-1" aria-label="Mobile navigation">
          {navLinks.map((l) => (
            <a
              key={l.href}
              href={currentRoute === 'home' ? l.href : `/${l.href}`}
              onClick={(e) => handleNavClick(e, l.href)}
              className="px-3 py-2.5 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] rounded-lg hover:bg-[var(--color-surface)] transition-all cursor-pointer"
            >
              {l.label}
            </a>
          ))}
          <a
            href="/privacy"
            onClick={(e) => {
              if (!e.metaKey && !e.ctrlKey) {
                e.preventDefault()
                setMenuOpen(false)
                onNavigate?.('privacy')
              }
            }}
            className={`px-3 py-2.5 text-sm rounded-lg transition-all cursor-pointer ${
              currentRoute === 'privacy'
                ? 'text-[var(--color-accent-teal)] font-medium bg-[var(--color-surface)]'
                : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface)]'
            }`}
          >
            Privacy Policy
          </a>
          <a
            href="/terms"
            onClick={(e) => {
              if (!e.metaKey && !e.ctrlKey) {
                e.preventDefault()
                setMenuOpen(false)
                onNavigate?.('terms')
              }
            }}
            className={`px-3 py-2.5 text-sm rounded-lg transition-all cursor-pointer ${
              currentRoute === 'terms'
                ? 'text-[var(--color-accent-lavender)] font-medium bg-[var(--color-surface)]'
                : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface)]'
            }`}
          >
            Terms of Service
          </a>
          <div className="mt-2 pt-2 border-t border-[var(--color-border)] flex flex-col gap-2">
            <a
              href="https://github.com/chappidiRushi/vittify"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 px-3 py-2.5 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] rounded-lg hover:bg-[var(--color-surface)] transition-all"
            >
              <Github size={16} />
              GitHub
            </a>
            <a
              href="https://github.com/chappidiRushi/vittify/releases"
              target="_blank"
              rel="noopener noreferrer"
              className="px-3 py-2.5 text-sm font-medium text-center bg-[var(--color-accent-coral)] text-white rounded-full hover:opacity-90 transition-opacity"
            >
              Get App
            </a>
          </div>
        </nav>
      </div>
    </header>
  )
}
