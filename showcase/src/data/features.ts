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
      'Built for pure convenience and daily ease of use: Vittify reads incoming bank SMS alerts entirely on-device using 157+ bank parsers across 24+ countries. Zero manual effort needed. Every debit and credit is captured, categorized, and stored locally the moment it arrives.',
    screenshot: 'home.png',
    screenshotAlt: 'Vittify home screen showing financial overview and recent transactions',
    tag: '157+ Banks',
    tagColor: 'coral',
    bullets: [
      'Built for convenience — 100% automatic hands-free logging',
      'On-device SMS parser engine — never uploads bank messages',
      '157+ financial institutions across 24+ countries',
      'Instant UPI statement PDF import (GPay, PhonePe, Paytm)',
    ],
  },
  {
    id: 'receipt-scanner',
    label: 'Receipt OCR & Split',
    headline: 'Snap paper bills. Extract line items & split easily.',
    description:
      'Effortless expense capture from paper and digital receipts. Use Google ML Kit for instant on-device text recognition, or harness optional Gemini AI to parse line items, taxes, and discounts into structured split transactions. Attach receipt photos directly to transactions for effortless record keeping.',
    screenshot: 'transaction_detail.png',
    screenshotAlt: 'Vittify physical receipt slip and transaction details with item breakdown',
    tag: 'ML Kit + AI OCR',
    tagColor: 'teal',
    bullets: [
      'On-device OCR with Google ML Kit — zero cloud upload latency',
      'Optional Gemini AI line-item extraction for complex receipts',
      'Itemized split transactions across different categories',
      'Document scanner & bill photo attachments for warranties',
    ],
  },
  {
    id: 'convenience-flow',
    label: '1-Tap Convenience',
    headline: 'Engineered for speed, simplicity, and ease of use.',
    description:
      'Tracking money shouldn’t feel like a chore. Pin the glanceable Home Screen Quick Add widget or the dedicated Scan Receipt widget to log cash, coffee, or transit fares in two taps without opening the app. Use plain-English natural language typing or smart subcategory search for lightning-fast entry.',
    screenshot: 'add_transaction.png',
    screenshotAlt: 'Vittify tactile quick add screen with rolling amount numbers',
    tag: 'Built for Speed',
    tagColor: 'amber',
    bullets: [
      'Home Screen Quick Add widget for instant 2-tap cash logging',
      'Dedicated Scan Receipt launcher widget for camera capture',
      'Plain-English quick entry (e.g., "₹450 groceries at Blinkit")',
      'Comprehensive subcategory item catalogue for instant autocomplete',
    ],
  },
  {
    id: 'timeline',
    label: 'Timeline & Receipts',
    headline: 'Your financial story, beautifully told.',
    description:
      'Date-grouped transaction platters with live net totals, quick filter chips, and instant global search with live multi-currency conversion. Every transaction generates a tactile physical receipt slip with clean metadata, making financial review delightfully effortless.',
    screenshot: 'transactions.png',
    screenshotAlt: 'Transactions timeline grouped by date with running totals',
    tag: 'Scannable',
    tagColor: 'lavender',
    bullets: [
      'Date-grouped 28dp squircle platters with tactile haptics',
      'Live credit and debit net totals at a glance',
      'Quick filter chips: All · Expense · Income · Transfer · Investment',
      'Global multi-currency search with live exchange rate estimates',
    ],
  },
  {
    id: 'budgets',
    label: 'Smart Budgets',
    headline: 'Know exactly where you stand today.',
    description:
      'Set monthly budgets per category with total ease. Visual progress bars, daily remaining allowances, and days-left indicators tell you at a glance whether you\'re on track — zero mental arithmetic required.',
    screenshot: 'budgets.png',
    screenshotAlt: 'Monthly budget tracking with visual progress bars per category',
    tag: 'Zero Math',
    tagColor: 'mint',
    bullets: [
      'Per-category monthly budget caps with customizable alerts',
      'Adaptive daily remaining allowance calculation',
      'Visual fill bars with intuitive 80% & 100% threshold warnings',
      'Granular category donut breakdown & historical budget trends',
    ],
  },
  {
    id: 'partner-sync',
    label: 'Couple Sync',
    headline: 'Share finances effortlessly. No server. No accounts.',
    description:
      'Convenience meets total privacy. Pair with a partner via QR code over encrypted local mesh networking (Nearby Connections + WebRTC). Your combined spending syncs device-to-device with zero cloud intermediary, giving you unified or separate timelines in one tap.',
    screenshot: 'partner_sync.png',
    screenshotAlt: 'Couple partner sync screen showing paired device and shared spending',
    tag: 'P2P Encrypted',
    tagColor: 'lavender',
    bullets: [
      '5-second instant QR-code device pairing',
      'End-to-end encrypted local peer-to-peer mesh sync',
      'No account registration, no server passwords, no cloud DB',
      'Combined "Both / Me / Partner" view toggle in top app bar',
    ],
  },
  {
    id: 'ai-integration',
    label: 'Gemini AI (BYOK)',
    headline: 'Intelligent assistance. Completely private & opt-in.',
    description:
      'Supercharge your expense tracking with Google Gemini AI using your own private API key. Experience plain-English spending resolution, receipt extraction, model selection (Gemini 2.5 Flash & Pro), and local token tracking — with zero third-party proxy servers.',
    screenshot: 'ai_integration.png',
    screenshotAlt: 'Vittify Gemini AI settings with model selection and token counter',
    tag: 'Optional Cloud AI',
    tagColor: 'coral',
    bullets: [
      'Bring Your Own Key (BYOK) — direct private Google API',
      'Dynamic model guidance: Gemini 2.5 Flash, 2.5 Pro & lightweight options',
      'Configurable custom prompts for plain-English expense logging',
      '100% optional: app remains completely functional and private offline',
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
    id: 'import-wizard',
    label: 'Smart Import & Restore',
    headline: 'Bring your financial history from anywhere.',
    description:
      'Zero lock-in. Migrate your complete financial ledger into Vittify with an interactive step-by-step wizard. Ingest Vittify backups, multi-sheet Excel (.xlsx) workbooks, Cashew SQLite database backups, or custom CSV files. Preview, select, and map accounts and categories before importing.',
    screenshot: 'backup_sync.png',
    screenshotAlt: 'Vittify restore and import wizard showing multiple sources and preview mapping',
    tag: 'Excel · Cashew · CSV',
    tagColor: 'mint',
    bullets: [
      'Multi-source ingestion — Vittify JSON/ZIP, Excel (.xlsx), Cashew (.sqlite), and CSV',
      'Downloadable sample Excel & CSV templates with dynamic dependent dropdowns',
      'Interactive preview with category auto-matching and account mapping',
      'Self-transfers & multi-currency handling with automated balance audit',
    ],
  },
  {
    id: 'analytics',
    label: 'Analytics & Heatmaps',
    headline: 'Insights that actually mean something.',
    description:
      'Interactive trend curves, savings rate tracking, subscription forecasting, and daily financial activity heatmaps. All computed locally with blazing speed. All instantly available.',
    screenshot: 'analytics.png',
    screenshotAlt: 'Analytics overview with trend charts and category breakdowns',
    tag: 'Local Analytics',
    tagColor: 'teal',
    bullets: [
      'Interactive period trend curves and savings rate tracking',
      'GitHub-style daily financial activity spending heatmap',
      'Recurring subscription projections (monthly & annual)',
      'Account-level running ledger and balance history curves',
    ],
  },
]
