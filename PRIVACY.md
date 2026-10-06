# Privacy Policy

**Last Updated: September 2026**

## Our Commitment to Privacy

Vittify is built with privacy as the core principle. We believe your financial data should remain yours alone.

## 100% On-Device Processing

**All data processing happens locally on your device.** We use MediaPipe's on-device LLM (Qwen 2.5) for AI features, ensuring:

- ✅ **No cloud servers** - Your data never leaves your phone
- ✅ **No data collection** - We don't collect, store, or transmit any user data
- ✅ **No tracking** - No analytics, no telemetry, no user tracking
- ✅ **No ads** - No advertising networks or tracking pixels
- ✅ **Offline AI** - Once downloaded, AI works completely offline

## Data Storage

### What We Store (Locally Only)
- Transaction details extracted from SMS (amount, merchant, date, category)
- Your custom categories and notes
- App preferences and settings

### Where It's Stored
- All data is stored in a local SQLite database on your device
- Database is protected by Android's app sandboxing
- Data is only accessible to Vittify app

### Data Deletion & Export
- **Export Anytime**: You can export your full transaction database in JSON or CSV format, or generate a PDF statement whenever you desire.
- **Delete All Data**: A single tap in Settings > Data Privacy allows you to wipe all databases, accounts, preferences, and cached records completely and permanently from your phone.
- Uninstalling the app completely removes all data.

## Permissions

### SMS Permission (Read-Only)
- **Purpose**: To read bank transaction SMS messages
- **Scope**: Read-only access, we cannot send or modify messages
- **Processing**: The parser selectively looks only at messages sent from verified financial alphanumeric sender headers (such as AD-HDFCBK, VK-SBIINB, etc.).
- **Filtering**: Personal messages, OTPs, family and friend chats, and non-financial text messages are strictly filtered out and never read, stored, or retained.
- **Storage**: Only transaction data is extracted and stored, not full messages.

### Internet Permission
- **Primary Purpose**: To download the AI model (Qwen 2.5) on first use
- **Model Download**: One-time download of ~1.5GB model file from CloudFront CDN
- **App Updates**: Google Play Store variant uses Play Services for app updates (F-Droid variant does not)
- **After Model Download**: AI works completely offline, no internet required for core features
- **Your Data**: Never transmitted over the internet, all processing remains on-device

### Camera Permission (Optional)
- **Purpose**: To scan QR codes during optional Couple Sync pairing
- **Scope**: Used only while the pairing QR scanner is actively opened
- **Processing**: Frames are scanned purely in memory to decode the pairing key; no images or videos are ever recorded, saved, or transmitted

### Nearby Devices & Local Network Permissions (Optional)
- **Purpose**: To discover and connect to your partner's device for direct peer-to-peer synchronization
- **Scope**: Used only when Couple Sync is configured and enabled
- **Security**: Communication occurs directly over your local network or WebRTC peer connections, encrypted end-to-end; no intermediary signaling servers retain data.

### Device Biometrics & App Security (Optional)
- **Purpose**: Protect access to the app via fingerprint or face recognition
- **Implementation**: Utilizes Android's hardware-backed BiometricPrompt API
- **Security**: Biometric template data is processed directly by the Android operating system's Trusted Execution Environment (TEE). The app never has access to raw biometric data.

### No Unnecessary Permissions
- No location tracking
- No contact access
- No microphone access

## Third-Party Services & Optional Cloud Features

Vittify does **NOT** use:
- ❌ Analytics services (Google Analytics, Firebase, Facebook SDK, Adjust, AppsFlyer)
- ❌ Crash reporting SDKs or telemetry trackers
- ❌ Advertising networks or data brokers
- ❌ Social media SDKs
- ❌ Proprietary backend tracking servers

### Cloud Backup & WebDAV (Client-Side Encryption)
- Cloud backups are strictly opt-in and controlled entirely by you.
- If you choose to enable Google Drive or WebDAV sync, backups are packaged as encrypted JSON archives using standard AES encryption and your chosen secret passphrase.
- The app communicates directly with your personal cloud account; Vittify does not intermediate or retain any keys or tokens.

### Optional AI Modes
- **On-Device LLM (Default)**: Runs 100% locally via MediaPipe LLM Inference. Your data never leaves your device.
- **Bring-Your-Own-Key (BYOK) Cloud AI (Optional)**: If you choose to configure a personal Google Gemini API key, prompts are sent directly from your device to the Google Gemini API. Your API key is encrypted and stored securely on your device using Android Keystore/EncryptedSharedPreferences. No intermediary servers are involved.

**Note**: The Google Play Store variant includes Play Services for app updates only. The F-Droid variant has no Google services.

## AI Features

### On-Device AI Assistant
- Uses MediaPipe's Qwen 2.5 model (1.5GB download)
- Model runs entirely on your device using MediaPipe LLM Inference
- After initial download, no internet connection required
- Conversations are not stored or transmitted
- AI insights are generated locally from your local transaction data
- Model file stored in app's private storage

## Data Export

When you export your data:
- CSV/PDF files are created locally on your device
- You control where to share or save them
- No automatic uploads or backups

## Open Source Transparency

Vittify is fully open source:
- Review our code
- Verify our privacy claims yourself
- Contribute to make it even better

## Children's Privacy

Vittify is not directed at children under 13. We do not knowingly collect information from children.

## Changes to Privacy Policy

Any changes to this privacy policy will be:
- Updated in the app repository and showcase website
- Reflected in the "Last Updated" date
- Communicated through release notes

## Contact

For privacy concerns, questions, or feedback:
- **Email**: [thegodscode@gmail.com](mailto:thegodscode@gmail.com)
- **GitHub**: [Open an issue on GitHub](https://github.com/chappidiRushi/vittify/issues)

## Summary

**Your financial data stays on your phone. Period.**

- No servers
- No uploads
- No tracking
- No ads
- Complete privacy

---

*Vittify - Privacy-first expense tracking with on-device AI*