export interface FAQItem {
  q: string
  a: string
}

export const faqs: FAQItem[] = [
  {
    q: 'Is Vittify available on the Play Store?',
    a: 'Vittify is currently in active development and not yet on the Play Store. You can build and sideload it from source on GitHub. A Play Store release is planned — watch the repo for updates.',
  },
  {
    q: 'How does SMS reading work — is it safe?',
    a: 'Vittify requests the READ_SMS permission and parses your bank messages entirely on-device using its built-in parser engine. No message content is ever uploaded to any server. The parsing happens locally, results are stored in an encrypted local Room database, and that\'s the end of the data journey.',
  },
  {
    q: 'Can it import bank statements or UPI PDFs?',
    a: 'Yes. You can import Google Pay, PhonePe, and Paytm UPI PDF statements, as well as standard bank PDF exports. Vittify uses PdfBox to extract and parse transaction data locally, with no third-party processing.',
  },
  {
    q: 'Does Vittify sync with any cloud service?',
    a: 'There is no automatic cloud sync for your live data. Backup (to Google Drive, Nextcloud, or WebDAV) is a separate, fully opt-in feature — and the backup file is AES-256 encrypted on your device before upload, so the storage provider only ever sees ciphertext.',
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
    a: 'Vittify uses a QR code handshake to exchange encryption keys between two devices on the same local network. After pairing, transactions sync directly device-to-device over encrypted WebRTC data channels and Google Nearby Connections. No intermediary server is involved at any point.',
  },
  {
    q: 'Which banks are supported?',
    a: 'Vittify includes 155+ parsers covering banks across India, the US, UK, UAE, Malaysia, Singapore, Philippines, Kenya, Pakistan, Bangladesh, Germany, Turkey, Russia, and more. If your bank isn\'t supported yet, you can contribute a parser — it\'s a few dozen lines of Kotlin.',
  },
  {
    q: 'Is Vittify free and open source?',
    a: 'Yes. Vittify is fully open source under the AGPL-3.0 license. You can inspect every line of code, build it yourself, fork it, and contribute. There are no paid tiers, no paywalls, and no feature locks.',
  },
  {
    q: 'Can I add transactions manually?',
    a: 'Absolutely. The streamlined Add Transaction screen lets you log cash purchases, UPI transactions not caught by SMS, or any arbitrary transaction. The plain-English Quick Add (beta) also lets you type phrases like "₹450 groceries at Blinkit via HDFC" to auto-fill entries.',
  },
  {
    q: 'Does Vittify use AI?',
    a: 'Optionally. Vittify integrates Google Gemini AI for the Quick Add "plain-English" input feature. This is opt-in, uses your own API key (BYOK), and is the only feature that ever makes an outbound network call — and only when you explicitly trigger it.',
  },
  {
    q: 'How is my data secured on the device?',
    a: 'The local Room database is encrypted using AES-256 keys stored in the Android Keystore (hardware-backed where available). The app supports biometric lock. Partner Sync keys are exchanged over a local encrypted handshake and stored securely per-session.',
  },
]
