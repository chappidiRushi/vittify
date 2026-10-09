export interface Feature {
  id: string
  label: string
  headline: string
  description: string
  screenshot: string
  screenshotAlt: string
  tag: string
  tagColor: string
  bullets: string[]
}

export const features: Feature[] = [
  {
    id: 'sms-parsing',
    label: 'Auto Capture',
    headline: 'Your transactions, captured automatically.',
    description:
      'Vittify reads incoming bank SMS alerts entirely on-device using 155+ bank parsers. No manual entry. Every debit and credit is captured, categorized, and stored locally the moment it arrives. Optional cloud AI integration is available.',
    screenshot: 'home.png',
    screenshotAlt: 'Vittify home screen showing financial overview and recent transactions',
    tag: '155+ Banks',
    tagColor: 'coral',
    bullets: [
      'On-device SMS parser — never leaves your phone',
      '155+ banks across 30+ countries',
      'Auto-categorization with smart rules',
      'Instant UPI statement PDF import',
    ],
  },
  {
    id: 'timeline',
    label: 'Timeline',
    headline: 'Your financial story, beautifully told.',
    description:
      'Date-grouped transaction platter with live net totals, quick filters, and instant global search. The interface feels like a ledger that actually respects your time.',
    screenshot: 'transactions.png',
    screenshotAlt: 'Transactions timeline grouped by date with running totals',
    tag: 'Scannable',
    tagColor: 'lavender',
    bullets: [
      'Date-grouped squircle platters',
      'Live credit / debit net totals',
      'Quick filter chips: All · Expense · Income · Transfer',
      'Global search across all transactions',
    ],
  },
  {
    id: 'budgets',
    label: 'Budgets',
    headline: 'Know exactly where you stand today.',
    description:
      'Set monthly budgets per category. Visual progress bars, daily allowances, and days-remaining indicators tell you at a glance whether you\'re on track — no mental math required.',
    screenshot: 'budgets.png',
    screenshotAlt: 'Monthly budget tracking with visual progress bars per category',
    tag: 'Smart Budgets',
    tagColor: 'mint',
    bullets: [
      'Per-category monthly budget limits',
      'Daily remaining allowance calculation',
      'Visual fill bars with threshold warnings',
      'Granular category donut breakdown',
    ],
  },
  {
    id: 'partner-sync',
    label: 'Couple Sync',
    headline: 'Share finances. No server. No accounts.',
    description:
      'Pair with a partner via QR code over encrypted local mesh networking (Nearby Connections + WebRTC). Your combined spending syncs device-to-device with zero cloud intermediary.',
    screenshot: 'partner_sync.png',
    screenshotAlt: 'Couple partner sync screen showing paired device and shared spending',
    tag: 'P2P Encrypted',
    tagColor: 'lavender',
    bullets: [
      'Instant QR-code device pairing',
      'End-to-end encrypted local sync',
      'No accounts, no server, no cloud DB',
      'Combined "Both / Me / Partner" view toggle',
    ],
  },
  {
    id: 'cloud-backup',
    label: 'Cloud Backup',
    headline: 'Your data, backed up on your terms.',
    description:
      'Vittify supports encrypted automatic backups to Google Drive, Nextcloud, or any WebDAV server. Your backup file is AES-256 encrypted before it ever leaves your device — the cloud storage provider sees only opaque ciphertext. Restore on any device with your passphrase.',
    screenshot: 'backup_sync.png',
    screenshotAlt: 'Vittify Backup & Sync screen showing Local, Nextcloud/WebDAV, and Google Drive options',
    tag: 'Opt-in Encrypted',
    tagColor: 'amber',
    bullets: [
      'Google Drive — one-tap Google account link, auto daily/weekly schedule',
      'Nextcloud & WebDAV — self-host your own backup destination',
      'AES-256 client-side encryption before upload — provider sees only ciphertext',
      'Full restore from backup with your passphrase on any device',
    ],
  },
  {
    id: 'analytics',
    label: 'Analytics',
    headline: 'Insights that actually mean something.',
    description:
      'Interactive trend curves, savings rate tracking, subscription projections, and account-level breakdowns. All computed locally. All instantly available.',
    screenshot: 'analytics.png',
    screenshotAlt: 'Analytics overview with trend charts and category breakdowns',
    tag: 'Local Analytics',
    tagColor: 'teal',
    bullets: [
      'Interactive period trend curves',
      'Savings rate and net worth tracking',
      'Subscription projections (monthly / yearly)',
      'Budget history and spending heatmap',
    ],
  },
]
