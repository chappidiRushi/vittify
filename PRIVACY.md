# Privacy Policy

**Last Updated: October 2026**

## Our Commitment to Privacy

> **"Your money is your business. Vittify runs 100% on your device, contains zero ads, zero trackers, and never uploads your personal data to remote servers."**

Vittify is built with privacy as its primary founding principle. We believe personal financial data should remain strictly confidential and solely under your control.

---

### Key Highlights

- 🛡️ **On-Device Processing**: Financial calculations, categorization, and parsing execute exclusively on your phone (with optional cloud AI integration).
- 🚫 **Zero Trackers & Zero Ads**: No analytics SDKs, no telemetry, no tracking pixels, and no commercial profiling.
- 🔐 **Client-Side AES Encryption**: Optional backups are encrypted on-device before transmission.
- 📡 **P2P Encrypted Sync**: Couple and partner sync communicates directly device-to-device without cloud databases.

---

## 1. Core Philosophy: On-Device Parsing, Optional Cloud AI

- **Local-First Architecture**: Vittify is fundamentally engineered as a local-first, privacy-focused financial companion.
- **Hardware-Level Processing**: All transaction parsing, financial calculations, categorization, and account balance computations occur exclusively on your device's hardware.
- **No Backend Infrastructure**: We do not operate backend user databases, tracking endpoints, or cloud analytics pipelines. Your financial reality remains yours alone.
- **Full Offline Utility**: All core features work completely offline without transmitting your financial records to remote servers.

---

## 2. SMS Permission & Usage Scope

- **Strictly Financial Reading**: Vittify requests `READ_SMS` and `RECEIVE_SMS` permissions solely to detect transactional debit and credit alerts from financial institutions.
- **Alphanumeric Header Filtering**: The parser selectively inspects only messages sent from verified financial alphanumeric sender headers (such as `AD-HDFCBK`, `VK-SBIINB`, `AX-ICICIB`).
- **Exclusion of Personal Chats**: Personal messages, OTP verification codes, family and friend chats, and non-financial text messages are strictly filtered out and never read, stored, or retained.
- **No Message Transmission**: SMS message text never leaves your smartphone under any circumstance. Only parsed transaction metadata (amount, merchant, category, timestamp) is saved to your local database.

---

## 3. Zero Remote Tracking & No Analytics SDKs

- **No Analytics Frameworks**: Vittify does **NOT** include Google Analytics, Firebase Analytics, Facebook SDK, Adjust, AppsFlyer, or any commercial telemetry trackers.
- **No Device Profiling**: No advertising identifiers (AAID) or hardware fingerprints are collected or shared with third parties.
- **Ad-Free Experience**: There are zero advertisements, promotional tracking pixels, or data-broker integrations anywhere in the application.

---

## 4. Optional Cloud Backup (Google Drive & WebDAV)

- **Strictly Opt-In**: Cloud backups are completely optional, disabled by default, and controlled entirely by you.
- **Google Drive Integration**: When Google Drive sync is enabled, Vittify requests access strictly to its own hidden Application Data folder (`https://www.googleapis.com/auth/drive.appdata`). Vittify cannot view, access, or modify any other files, folders, documents, or photos in your Google Drive.
- **Client-Side AES-256 Encryption**: Backups are packaged as encrypted archives using standard AES-256 encryption with your chosen private passphrase before leaving your device. Cloud storage providers see only opaque ciphertext and cannot read your transactions or account balances.
- **Direct Device-to-Cloud Connection**: The app connects directly to Google Drive or your private WebDAV endpoint; Vittify does not run intermediary servers, relays, or proxies, and never receives or stores your credentials or data.
- **Google API Limited Use Disclosure**: Vittify's use and transfer of information received from Google APIs adheres to the [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy), including the **Limited Use** requirements. Your data is never sold, never transferred to third parties, never used for advertising, and never used to train generalized AI or machine learning models.
- **Complete User Control**: You can disconnect Google Drive or permanently delete your stored backup snapshots at any time directly in the app settings.

---

## 5. Device Biometrics & App Security

- **Operating System TEE**: App Lock utilizes Android's hardware-backed `BiometricPrompt` API (Fingerprint and Face Unlock).
- **Zero Raw Biometric Access**: Biometric template data is processed directly by the Android operating system's Trusted Execution Environment (TEE). The app never has access to raw biometric data.
- **Protected Local Storage**: Local database files are protected by Android app sandboxing and device encryption at rest.

---

## 6. Peer-to-Peer Device Sync & WebRTC

- **Direct Communication**: When using Couple Tracker or P2P Device Sync, communication between devices occurs directly over your local Wi-Fi network or WebRTC peer data channels.
- **End-to-End Encryption**: Data transferred during peer synchronization is encrypted end-to-end and is never retained by any signaling server.
- **Granular Control**: You have granular control over which accounts or tags are shared with your partner.

---

## 7. User Rights & Complete Data Control

- **Export Anytime**: You can export your full transaction database in JSON or CSV format, or generate a PDF statement whenever you desire.
- **Delete All Data**: A single tap in **Settings > Data Privacy** allows you to wipe all databases, accounts, preferences, and cached records completely and permanently from your phone.
- **Clean Uninstallation**: Uninstalling the app cleanly removes all stored records and preferences automatically.

---

## 8. Optional Gemini AI Integration (Beta)

- **Beta Integration**: Vittify includes an optional Google Gemini integration that is currently in beta.
- **Completely Optional**: AI features are disabled by default. All core parsing, transaction management, and analytics operate fully offline without AI.
- **User-Initiated Processing**: If you choose to configure and use the Gemini beta (such as for natural language transaction quick-add via Bring-Your-Own-Key), only the specific input you enter is sent directly from your device to the Google Gemini API.
- **No Third-Party Intermediaries**: Your API key is encrypted and stored locally on your device via Android Keystore / EncryptedSharedPreferences. No intermediary proxy servers or telemetry trackers are involved.

---

## 9. Children's Privacy & Open Source Auditing

- **Children's Privacy**: Vittify is not directed toward children under the age of 13 and does not knowingly collect any data from minors.
- **Open Source Transparency**: Every line of Vittify source code is publicly accessible on GitHub under the AGPL-3.0 license. You can inspect, audit, and compile the code yourself.

---

## 10. Policy Updates & Inquiries

- **Policy Changes**: Any updates to this privacy policy will be reflected with a revised "Last Updated" date in the repository and showcase website.
- **Contact**: For privacy concerns, audits, or questions, reach out directly:
  - **Email**: [thegodscode@gmail.com](mailto:thegodscode@gmail.com)
  - **GitHub Repository**: [chappidiRushi/vittify](https://github.com/chappidiRushi/vittify)