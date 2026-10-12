export interface FAQItem {
  q: string
  a: string
}

export const faqs: FAQItem[] = [
  {
    q: 'Is Vittify ready to use?',
    a: 'Yes! Vittify is ready for daily expense tracking. You can sideload the APK directly from our GitHub Releases or build from source. Because Vittify is an actively evolving open-source project supporting hundreds of banks, you might occasionally encounter an edge case or minor bug. If you find any issues, please report them on GitHub Issues — we investigate reports promptly and will fix them as soon as possible!',
  },
  {
    q: 'How is Vittify built for convenience and ease of use?',
    a: 'Everything in Vittify is engineered to eliminate the tedious friction of manual tracking. Bank SMS notifications are parsed automatically on-device the moment you spend. For cash or manual entries, our glanceable Home Screen Quick Add widget and dedicated Scan Receipt launcher widget let you log or scan expenses in two taps without launching the app. You can also type natural English (e.g., "₹450 groceries at Blinkit via HDFC") or search a rich subcategory catalogue that autocompletes items in milliseconds.',
  },
  {
    q: 'How does Receipt Scanning & Split Items work?',
    a: 'Vittify provides instant receipt capture. Using Google ML Kit OCR on-device, it detects amounts, merchants, and dates from paper receipts and invoices with zero cloud dependency. With an optional Google Gemini AI key, it automatically extracts line items, taxes, and discounts, allowing you to split complex receipts (like groceries vs. home goods) across multiple categories or with friends in seconds.',
  },
  {
    q: 'Is Vittify available on the Play Store?',
    a: 'Vittify is currently available via GitHub Releases (pre-built APKs) and can be built from source. A Google Play Store and F-Droid release is in progress — star and watch the GitHub repository for release announcements.',
  },
  {
    q: 'How does SMS reading work — is it safe?',
    a: 'Vittify requests the READ_SMS permission and parses your bank messages entirely on-device using its built-in Kotlin parser engine. Message content is never uploaded to any server. Parsing happens locally in milliseconds, and the results are stored in an encrypted local Room database backed by Android Keystore.',
  },
  {
    q: 'Can it import bank statements or UPI PDFs?',
    a: 'Yes. You can import Google Pay, PhonePe, and Paytm UPI PDF statements, as well as standard bank PDF exports. Vittify uses PdfBox to extract and parse transaction data locally on your device, with no third-party servers involved.',
  },
  {
    q: 'Does Vittify sync with any cloud service?',
    a: 'There is no automatic cloud sync for your live data. Live transactions stay strictly on your phone. Backups (to Google Drive, Nextcloud, or WebDAV) are completely optional and opt-in — and all backup files are encrypted on your phone with client-side AES-256 before upload. Storage providers only ever see ciphertext.',
  },
  {
    q: 'How does Google Drive backup work?',
    a: 'In Settings → Backup, link your Google account. Vittify encrypts your entire database with a passphrase you choose using AES-256, then uploads the encrypted archive to your own Google Drive storage (your quota, your account). No Vittify server is involved — the upload goes directly from your device to Google Drive. You can schedule daily or weekly automatic backups.',
  },
  {
    q: 'Can I back up to my own server (Nextcloud / WebDAV)?',
    a: 'Yes. Vittify supports any WebDAV endpoint — Nextcloud, ownCloud, Synology, Hetzner StorageBox, or a custom self-hosted server. Enter your WebDAV URL, username, and password in Settings → Backup. The same AES-256 client-side encryption applies — your server never receives unencrypted data.',
  },
  {
    q: 'How does Partner Sync work without a server?',
    a: 'Vittify uses a simple QR code handshake to exchange encryption keys between two phones. After pairing, transactions sync directly device-to-device over encrypted WebRTC data channels and Google Nearby Connections. No intermediary server, account registration, or cloud database is involved at any point.',
  },
  {
    q: 'Which banks are supported?',
    a: 'Vittify includes 157+ parsers covering leading banks across India, the US, UK, UAE, Nepal, Thailand, Malaysia, Singapore, Philippines, Kenya, Nigeria, Saudi Arabia, Tanzania, Pakistan, Bangladesh, Germany, and more. If your bank isn\'t supported yet, you can contribute a parser in a few lines of Kotlin.',
  },
  {
    q: 'Is Vittify free and open source?',
    a: 'Yes. Vittify is 100% free and open source under the AGPL-3.0 license. You can inspect every line of code, build it yourself, fork it, and contribute. There are no paid tiers, no ads, no trackers, and no paywalls.',
  },
  {
    q: 'Can I add transactions manually or with plain English?',
    a: 'Absolutely. The streamlined Add Transaction screen features rolling-number heroes and tactile pickers. You can also use plain-English Quick Add (beta) to type phrases like "₹450 groceries at Blinkit via HDFC" or "$32 dinner with Sam" to auto-fill amount, category, account, and merchant.',
  },
  {
    q: 'Does Vittify use AI, and which models are supported?',
    a: 'AI is completely optional. Vittify supports Google Gemini via Bring Your Own Key (BYOK). You can configure your own private API key to use models like Gemini 2.5 Flash, Gemini 2.5 Pro, or lightweight options for plain-English quick adds and receipt line-item extraction. It includes offline token tracking, and the app remains 100% functional and offline without AI.',
  },
  {
    q: 'How is my data secured on the device?',
    a: 'The local Room database is protected using encryption keys stored in the Android Keystore (hardware-backed Keystore / StrongBox where available). The app supports biometric lock (Fingerprint / Face Unlock). Partner Sync keys are exchanged over a local encrypted handshake.',
  },
]
