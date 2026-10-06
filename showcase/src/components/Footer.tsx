import { Github, ArrowUp } from 'lucide-react'

interface FooterProps {
  onNavigate?: (route: 'home' | 'privacy' | 'terms', hash?: string) => void
}

const links = [
  {
    label: 'Product',
    items: [
      { label: 'Features', href: '#features', internal: true },
      { label: 'Screenshots', href: '#screenshots', internal: true },
      { label: 'How It Works', href: '#how-it-works', internal: true },
      { label: 'FAQ', href: '#faq', internal: true },
    ],
  },
  {
    label: 'Developers',
    items: [
      { label: 'GitHub Repository', href: 'https://github.com/chappidiRushi/vittify', external: true },
      { label: 'Contributing Guide', href: 'https://github.com/chappidiRushi/vittify/blob/main/CONTRIBUTING.md', external: true },
      { label: 'Parser Standards', href: 'https://github.com/chappidiRushi/vittify/blob/main/docs/parser-test-standards.md', external: true },
      { label: 'Open Issues', href: 'https://github.com/chappidiRushi/vittify/issues', external: true },
    ],
  },
  {
    label: 'Legal',
    items: [
      { label: 'Privacy Policy', href: '/privacy', internal: true, route: 'privacy' as const },
      { label: 'Terms of Service', href: '/terms', internal: true, route: 'terms' as const },
      { label: 'AGPL-3.0 License', href: 'https://github.com/chappidiRushi/vittify/blob/main/LICENSE', external: true },
      { label: 'Security Policy', href: 'https://github.com/chappidiRushi/vittify/blob/main/SECURITY.md', external: true },
      { label: 'Code of Conduct', href: 'https://github.com/chappidiRushi/vittify/blob/main/CODE_OF_CONDUCT.md', external: true },
    ],
  },
]

export function Footer({ onNavigate }: FooterProps) {
  const handleLinkClick = (
    e: React.MouseEvent<HTMLAnchorElement>,
    item: { href: string; external?: boolean; internal?: boolean; route?: 'privacy' | 'terms' }
  ) => {
    if (item.external || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) {
      return
    }

    if (item.route) {
      e.preventDefault()
      onNavigate?.(item.route)
      return
    }

    if (item.href.startsWith('#')) {
      e.preventDefault()
      onNavigate?.('home', item.href)
      return
    }

    if (item.href === '/') {
      e.preventDefault()
      onNavigate?.('home')
      return
    }
  }

  return (
    <footer className="bg-[var(--color-surface)] border-t border-[var(--color-border)]">
      <div className="max-w-6xl mx-auto px-6 py-16">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10">
          {/* Brand */}
          <div className="md:col-span-1">
            <a
              href="/"
              onClick={(e) => {
                if (!e.metaKey && !e.ctrlKey) {
                  e.preventDefault()
                  onNavigate?.('home')
                }
              }}
              className="flex items-center gap-2.5 mb-4 group cursor-pointer"
            >
              <div className="w-8 h-8 rounded-xl bg-[var(--color-accent-coral)] flex items-center justify-center text-white font-bold text-sm shadow-md group-hover:scale-105 transition-transform">
                V
              </div>
              <span className="font-bold text-[var(--color-text-primary)] text-base tracking-tight">
                Vittify
              </span>
            </a>
            <p className="text-sm text-[var(--color-text-secondary)] leading-relaxed mb-5">
              A tiny, smart financial companion that makes tracking money surprisingly delightful.
            </p>
            <a
              href="https://github.com/chappidiRushi/vittify"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 text-xs text-[var(--color-text-tertiary)] hover:text-[var(--color-text-secondary)] transition-colors"
              aria-label="View Vittify on GitHub"
            >
              <Github size={14} />
              chappidiRushi/vittify
            </a>
          </div>

          {/* Link columns */}
          {links.map((col) => (
            <div key={col.label}>
              <p className="text-xs font-semibold tracking-widest uppercase text-[var(--color-text-tertiary)] mb-4">
                {col.label}
              </p>
              <ul className="flex flex-col gap-2.5">
                {col.items.map((item) => (
                  <li key={item.label}>
                    <a
                      href={item.href}
                      onClick={(e) => handleLinkClick(e, item)}
                      target={'external' in item && item.external ? '_blank' : undefined}
                      rel={'external' in item && item.external ? 'noopener noreferrer' : undefined}
                      className="text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors cursor-pointer"
                    >
                      {item.label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Bottom bar */}
        <div className="mt-12 pt-6 border-t border-[var(--color-border)] flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-xs text-[var(--color-text-tertiary)] text-center sm:text-left">
            © 2026 Vittify. Open source under{' '}
            <a
              href="https://github.com/chappidiRushi/vittify/blob/main/LICENSE"
              target="_blank"
              rel="noopener noreferrer"
              className="underline underline-offset-2 hover:text-[var(--color-text-secondary)] transition-colors"
            >
              AGPL-3.0
            </a>
            . Forked from{' '}
            <a
              href="https://github.com/ritesh-kanwar/Cashiro"
              target="_blank"
              rel="noopener noreferrer"
              className="underline underline-offset-2 hover:text-[var(--color-text-secondary)] transition-colors"
            >
              Cashiro
            </a>{' '}
            by ritesh-kanwar.
          </p>
          <button
            onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
            className="flex items-center gap-1.5 text-xs text-[var(--color-text-tertiary)] hover:text-[var(--color-text-secondary)] transition-colors cursor-pointer"
            aria-label="Back to top"
          >
            <ArrowUp size={12} />
            Back to top
          </button>
        </div>
      </div>
    </footer>
  )
}
