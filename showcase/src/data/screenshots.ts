export interface ScreenshotItem {
  file: string
  caption: string
  description: string
}

export const screenshots: ScreenshotItem[] = [
  {
    file: 'home.png',
    caption: 'Home Dashboard',
    description: 'Financial overview, bank balances, recent spend',
  },
  {
    file: 'transactions.png',
    caption: 'Transaction Timeline',
    description: 'Date-grouped records with live net totals & quick filters',
  },
  {
    file: 'transaction_detail.png',
    caption: 'Receipt Slip',
    description: 'Tactile receipt design with account & tag metadata',
  },
  {
    file: 'search.png',
    caption: 'Global Search',
    description: 'Instant multi-currency queries with live conversions',
  },
  {
    file: 'analytics.png',
    caption: 'Analytics Overview',
    description: 'Interactive trend curves, savings rate & period toggles',
  },
  {
    file: 'subscriptions.png',
    caption: 'Subscriptions Platter',
    description: 'Monthly/yearly projections & service commitments',
  },
  {
    file: 'budgets.png',
    caption: 'Monthly Budgets',
    description: 'Visual progress bars, daily allowances & days left',
  },
  {
    file: 'budget_details.png',
    caption: 'Budget Breakdown',
    description: 'Granular category donut & linked transactions',
  },
  {
    file: 'budget_history.png',
    caption: 'Budget History',
    description: 'Past months at a glance — track budget trends over time',
  },
  {
    file: 'partner_sync.png',
    caption: 'Partner Sync',
    description: 'Serverless encrypted P2P pairing via Nearby & WebRTC',
  },
  {
    file: 'partner_qr.png',
    caption: 'QR Pairing',
    description: 'Zero-setup device-to-device local key handshake',
  },
  {
    file: 'ai_integration.png',
    caption: 'Gemini Quick Add',
    description: 'Optional BYOK key with offline token tracking',
  },
  {
    file: 'categories.png',
    caption: 'Categories Hub',
    description: 'Tabs for Expense, Income, Credit & Investment',
  },
  {
    file: 'add_transaction.png',
    caption: 'Add Transaction',
    description: 'Rolling-number hero amount with tactile inputs',
  },
  {
    file: 'account_detail.png',
    caption: 'Account Detail',
    description: 'Per-account timeline and balance history',
  },
  {
    file: 'backup_sync.png',
    caption: 'Backup & Sync',
    description: 'Local, Nextcloud / WebDAV, & Google Drive cloud backup',
  },
  {
    file: 'settings.png',
    caption: 'Settings',
    description: 'Theme, privacy, sync, and appearance controls',
  },
]
