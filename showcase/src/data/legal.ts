export interface LegalSection {
  id: string
  title: string
  subtitle?: string
  paragraphs?: string[]
  points?: string[]
  badge?: string
}

export interface LegalDocument {
  title: string
  shortTitle: string
  effectiveDate: string
  manifesto: string
  summaryPills: { label: string; accent: string }[]
  sections: LegalSection[]
}

export const privacyPolicyData: LegalDocument = {
  title: 'Privacy Policy',
  shortTitle: 'Privacy',
  effectiveDate: 'October 2026',
  manifesto:
    'Your money is your business. Vittify runs 100% on your device, contains zero ads, zero trackers, and never uploads your personal data to remote servers.',
  summaryPills: [
    { label: '100% On-Device Processing', accent: 'var(--color-accent-teal)' },
    { label: 'Zero Trackers & Zero Ads', accent: 'var(--color-accent-mint)' },
    { label: 'Client-Side AES Encryption', accent: 'var(--color-accent-lavender)' },
    { label: 'P2P Encrypted Sync', accent: 'var(--color-accent-coral)' },
  ],
  sections: [
    {
      id: 'on-device-philosophy',
      title: '1. Core Philosophy: 100% On-Device Processing',
      subtitle: 'Your phone is the only server Vittify will ever need.',
      points: [
        'Vittify is fundamentally engineered as a local-first, privacy-focused financial companion.',
        'All transaction parsing, financial calculations, categorization, and account balance computations occur exclusively on your phone hardware.',
        'We do not operate backend user databases, tracking endpoints, or cloud analytics pipelines. Your financial reality remains yours alone.',
        'All core features work completely offline without transmitting your financial records to remote servers.',
      ],
    },
    {
      id: 'sms-permission',
      title: '2. SMS Permission & Usage Scope',
      subtitle: 'Strictly read-only access for verified banking notifications.',
      points: [
        'Vittify requests READ_SMS and RECEIVE_SMS permissions solely to detect transactional debit and credit alerts from financial institutions.',
        'The parser selectively inspects only messages sent from verified financial alphanumeric sender headers (e.g., AD-HDFCBK, VK-SBIINB, AX-ICICIB).',
        'Personal messages, OTP verification codes, family and friend chats, and non-financial text messages are strictly filtered out and never read, stored, or retained.',
        'SMS message text never leaves your smartphone under any circumstance. Only parsed transaction metadata (amount, merchant, category, timestamp) is saved to your local database.',
      ],
    },
    {
      id: 'zero-tracking',
      title: '3. Zero Remote Tracking & No Analytics SDKs',
      subtitle: 'No telemetry, no profiling, no advertising networks.',
      points: [
        'Vittify does NOT include Google Analytics, Firebase Analytics, Facebook SDK, Adjust, AppsFlyer, or any commercial telemetry trackers.',
        'No advertising identifiers (AAID) or hardware fingerprints are collected or shared with third parties.',
        'There are zero advertisements, promotional tracking pixels, or data-broker integrations anywhere in the application.',
      ],
    },
    {
      id: 'cloud-backup-drive',
      title: '4. Optional Cloud Backup (Google Drive & WebDAV)',
      subtitle: 'Optional client-side encrypted backups with strict limited scope.',
      points: [
        'Cloud backups are strictly opt-in, optional, and controlled entirely by you.',
        'Google Drive Integration: Vittify requests access strictly to its own hidden Application Data folder (drive.appdata). The app cannot view, access, or modify any other files in your Google Drive.',
        'Client-Side Encryption: Backups are packaged as encrypted archives using AES-256 encryption with your chosen secret passphrase before leaving your device.',
        'Direct Communication: The app connects directly to Google Drive or your private WebDAV endpoint; Vittify does not run intermediary servers and never stores your credentials or data.',
        'Google API Limited Use Disclosure: Vittify’s use and transfer of information received from Google APIs adheres to the Google API Services User Data Policy, including Limited Use requirements. Your data is never sold, transferred to third parties, used for advertising, or used to train AI models.',
        'Full Control: You can disconnect Google Drive or delete your stored backup snapshots at any time directly in the app settings.',
      ],
    },
    {
      id: 'device-biometrics',
      title: '5. Device Biometrics & App Security',
      subtitle: 'Hardware-backed biometric lock with zero raw biometric exposure.',
      points: [
        'App Lock utilizes Android hardware-backed BiometricPrompt API (Fingerprint and Face Unlock).',
        'Biometric template data is processed directly by the Android operating system Trusted Execution Environment (TEE). The app never has access to raw biometric data.',
        'Local database files are protected by Android app sandboxing and device encryption at rest.',
      ],
    },
    {
      id: 'p2p-sync',
      title: '6. Peer-to-Peer Device Sync & WebRTC',
      subtitle: 'Direct device-to-device synchronization for couples and shared budgets.',
      points: [
        'When using Couple Tracker or P2P Device Sync, communication between devices occurs directly over your local Wi-Fi network or WebRTC peer data channels.',
        'Data transferred during peer synchronization is encrypted end-to-end and never retained by any signaling server.',
        'You have granular control over which accounts or tags are shared with your partner.',
      ],
    },
    {
      id: 'user-rights-export',
      title: '7. User Rights & Complete Data Control',
      subtitle: 'Export your data anytime or permanently erase everything in one tap.',
      points: [
        'Export Anytime: You can export your full transaction database in JSON or CSV format, or generate a PDF statement whenever you desire.',
        'Delete All Data: A single tap in Settings > Data Privacy allows you to wipe all databases, accounts, preferences, and cached records completely and permanently from your phone.',
        'Uninstalling the app cleanly removes all stored records and preferences automatically.',
      ],
    },
    {
      id: 'ai-features',
      title: '8. Optional Gemini AI Integration (Beta)',
      subtitle: 'Experimental AI assistance available strictly on an opt-in basis.',
      points: [
        'Vittify includes an optional Google Gemini integration that is currently in beta.',
        'AI features are completely optional and disabled by default; all core parsing, financial management, and analytics operate fully offline without AI.',
        'If you choose to use the Gemini beta (such as for natural language transaction quick-add), only the text you explicitly enter is processed via the Gemini API.',
        'Your API key is stored securely on your device using Android Keystore / EncryptedSharedPreferences, with no intermediary servers or telemetry tracking.',
      ],
    },
    {
      id: 'children-and-open-source',
      title: "9. Children's Privacy & Open Source Auditing",
      subtitle: 'Fully auditable software under the AGPL-3.0 license.',
      points: [
        'Children’s Privacy: Vittify is not directed toward children under the age of 13 and does not knowingly collect any data from minors.',
        'Open Source Transparency: Every line of Vittify source code is publicly accessible on GitHub under AGPL-3.0. You do not need to take our word for it—you can inspect, audit, and compile the code yourself.',
      ],
    },
    {
      id: 'policy-contact',
      title: '10. Policy Updates & Inquiries',
      subtitle: 'Direct contact with maintainers for any privacy questions.',
      points: [
        'Any updates to this privacy policy will be reflected with a revised "Last Updated" date in the repository and showcase website.',
        'For privacy concerns, audits, or questions, reach out directly via email at thegodscode@gmail.com or open an issue on our GitHub repository.',
      ],
    },
  ],
}

export const termsOfServiceData: LegalDocument = {
  title: 'Terms of Service',
  shortTitle: 'Terms',
  effectiveDate: 'September 2026',
  manifesto:
    'Please read these terms carefully. Vittify is an open-source, local-first personal expense tracker provided free of charge for personal use. By using the app, you agree to these terms.',
  summaryPills: [
    { label: 'Free & Open Source (AGPL-3.0)', accent: 'var(--color-accent-teal)' },
    { label: 'Not Financial or Banking Advice', accent: 'var(--color-accent-amber)' },
    { label: 'User Controls Security & Keys', accent: 'var(--color-accent-lavender)' },
    { label: 'Provided "AS IS"', accent: 'var(--color-accent-coral)' },
  ],
  sections: [
    {
      id: 'acceptance',
      title: '1. Acceptance of Terms',
      subtitle: 'Binding agreement upon installation and usage.',
      paragraphs: [
        'By installing, accessing, or using Vittify, you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use the application.',
        'These terms apply to all users of the Vittify mobile application, showcase website, and related open-source documentation.',
      ],
    },
    {
      id: 'nature-and-disclaimer',
      title: '2. Nature of the Application & Financial Disclaimer',
      subtitle: 'An organizational estimation tool, not a certified financial advisor.',
      paragraphs: [
        'Vittify is an on-device personal expense tracker and organizational tool designed to help you monitor personal spending habits and budget allocations.',
        'Vittify does NOT provide certified financial advice, legal counsel, tax planning, investment recommendations, or banking services.',
        'Information displayed within the app—including currency exchange rates, budget projections, category estimates, and spending totals—is provided for convenience and estimation purposes only. Always refer to your official bank statements and institutional accounts for authoritative balances.',
      ],
    },
    {
      id: 'currency-exchange',
      title: '3. Currency Exchange Rates Notice & Disclaimer',
      subtitle: 'Informational rates for estimation only.',
      paragraphs: [
        'The exchange rates displayed within this app are for informational purposes only and should not be used for investment, trading, or currency arbitrage decisions. These rates are estimates and may not reflect real-time market rates.',
        'By using this app, you acknowledge that you understand and accept these limitations and that you assume full responsibility for any financial decisions made based on the information provided within the app.',
      ],
    },
    {
      id: 'sms-parsing-verification',
      title: '4. SMS Parsing & Data Verification',
      subtitle: 'Automated extraction requires user review.',
      paragraphs: [
        'Automatic transaction detection relies on heuristic and regex parsing of incoming SMS alerts sent by financial institutions. While our parsing algorithms are regularly tested and refined across dozens of banks, format variations, carrier changes, and network anomalies can occasionally result in misparsed or missed transactions.',
        'You are solely responsible for reviewing, verifying, and adjusting transactions, category assignments, and account balances generated by the app.',
      ],
    },
    {
      id: 'user-security',
      title: '5. User Security Responsibilities',
      subtitle: 'Securing your physical smartphone and private encryption keys.',
      paragraphs: [
        'Because Vittify operates with a local-first, serverless architecture, you are responsible for maintaining the physical and digital security of your device, operating system, and screen locks.',
        'If you configure encrypted cloud backups or WebDAV sync, you are solely responsible for safeguarding your encryption passphrase. Vittify operates without user accounts and cannot recover lost passphrases or decrypt backups without your secret key.',
        'We strongly recommend enabling App Lock (Biometrics or PIN) within the app settings to guard against unauthorized physical access to your device.',
      ],
    },
    {
      id: 'third-party-services',
      title: '6. Third-Party Services & Webhooks',
      subtitle: 'Terms governing external storage and integrations.',
      paragraphs: [
        'If you utilize optional third-party integrations—such as Google Drive for cloud backups, private WebDAV endpoints, or custom outgoing Webhooks—your use of those services is subject to their respective terms of service and privacy policies.',
        'Vittify is not responsible for the availability, security, uptime, or data handling practices of third-party cloud storage or automation providers.',
      ],
    },
    {
      id: 'open-source-software',
      title: '7. Open Source Software & Modifications',
      subtitle: 'Freedom to inspect and modify under AGPL-3.0.',
      paragraphs: [
        'Vittify is open-source software licensed under the GNU Affero General Public License v3.0 (AGPL-3.0). You are free to inspect, fork, or modify the source code under the applicable open-source license.',
        'Any custom builds, third-party forks, or modifications distributed by third parties are not the responsibility of the official Vittify project maintainers.',
      ],
    },
    {
      id: 'warranty-and-liability',
      title: '8. Warranty Disclaimer & Limitation of Liability',
      subtitle: 'Provided "AS IS" without warranties.',
      paragraphs: [
        'Vittify is provided on an "AS IS" and "AS AVAILABLE" basis, without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose, and non-infringement.',
        'To the maximum extent permitted by applicable law, the authors, maintainers, and contributors shall not be held liable for any direct, indirect, incidental, special, or consequential damages resulting from the use of or inability to use this software, including data loss, hardware failure, or financial decisions made based on app estimations.',
      ],
    },
    {
      id: 'terms-contact',
      title: '9. Contact & Inquiries',
      subtitle: 'Reach out for licensing or terms questions.',
      paragraphs: [
        'For inquiries regarding these Terms of Service, licensing, or community guidelines, contact thegodscode@gmail.com or visit github.com/chappidiRushi/vittify.',
      ],
    },
  ],
}
