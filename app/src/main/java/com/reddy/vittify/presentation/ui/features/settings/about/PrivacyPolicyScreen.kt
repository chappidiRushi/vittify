package com.reddy.vittify.presentation.ui.features.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.R
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

data class PolicySection(
    val title: String,
    val points: List<String>
)

private val POLICY_SECTIONS = listOf(
    PolicySection(
        title = "1. Core Philosophy: 100% On-Device Processing",
        points = listOf(
            "Vittify is fundamentally designed as a local-first, privacy-focused financial companion.",
            "All transaction parsing, financial calculations, categorization, and account balance computations happen exclusively on your device's hardware.",
            "We do not operate backend user databases, tracking endpoints, or cloud analytics pipelines. Your financial reality remains yours alone."
        )
    ),
    PolicySection(
        title = "2. SMS Permission & Usage",
        points = listOf(
            "Vittify requests READ_SMS and RECEIVE_SMS permissions solely to detect transactional debit and credit alerts from financial institutions.",
            "The parser selectively looks only at messages sent from verified financial alphanumeric sender headers (such as AD-HDFCBK, VK-SBIINB, etc.).",
            "Personal messages, OTPs, family and friend chats, and non-financial text messages are strictly filtered out and never read, stored, or retained.",
            "SMS message content never leaves your smartphone under any circumstance."
        )
    ),
    PolicySection(
        title = "3. Zero Remote Tracking & No Analytics SDKs",
        points = listOf(
            "Vittify does NOT include Google Analytics, Firebase Analytics, Facebook SDK, Adjust, AppsFlyer, or any commercial telemetry trackers.",
            "No advertising identifiers (AAID) are collected or shared with third parties.",
            "There are zero advertisements anywhere in the application."
        )
    ),
    PolicySection(
        title = "4. Optional Cloud Backup (Google Drive & WebDAV)",
        points = listOf(
            "Cloud backups are strictly opt-in, optional, and controlled entirely by you.",
            "Google Drive Integration: Access is strictly limited to Vittify's hidden Application Data folder (drive.appdata). The app cannot view, access, or modify any other files in your Google Drive.",
            "Client-Side Encryption: Backups are packaged as encrypted archives using AES-256 encryption with your chosen secret passphrase before leaving your device.",
            "Direct Communication: The app connects directly to Google Drive or your private WebDAV endpoint; Vittify does not run intermediary servers and never stores your credentials or data.",
            "Google API Limited Use: Vittify's use and transfer of information received from Google APIs adheres to the Google API Services User Data Policy, including Limited Use requirements.",
            "Full Control: You can disconnect Google Drive or delete your stored backup snapshots at any time directly in the app settings."
        )
    ),
    PolicySection(
        title = "5. Device Biometrics & App Security",
        points = listOf(
            "App Lock utilizes Android's hardware-backed BiometricPrompt API (Fingerprint / Face Unlock).",
            "Biometric template data is processed directly by the Android operating system's Trusted Execution Environment (TEE). The app never has access to raw biometric data."
        )
    ),
    PolicySection(
        title = "6. Peer-to-Peer Device Sync & WebRTC",
        points = listOf(
            "When using Couple Tracker or P2P Device Sync, communication between devices occurs directly over your local network or WebRTC peer connections.",
            "Data transferred during peer synchronization is encrypted end-to-end and never retained by any signaling server."
        )
    ),
    PolicySection(
        title = "7. User Rights & Complete Data Control",
        points = listOf(
            "Export Anytime: You can export your full transaction database in JSON or CSV format, or generate a PDF statement whenever you desire.",
            "Delete All Data: A single tap in Settings > Data Privacy allows you to wipe all databases, accounts, preferences, and cached records completely and permanently from your phone."
        )
    ),
    PolicySection(
        title = "8. Optional Gemini AI Integration (Beta)",
        points = listOf(
            "Vittify includes an optional Google Gemini integration that is currently in beta.",
            "AI features are completely optional and disabled by default; all core parsing and expense tracking functions operate fully offline without AI.",
            "If you choose to use the Gemini beta, only the text you explicitly enter is processed via the Gemini API without third-party telemetry."
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.privacy_policy),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .overScrollVertical()
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = paddingValues.calculateTopPadding(),
                    bottom = 24.dp
                ),
            flingBehavior = rememberOverscrollFlingBehavior { listState },
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Manifesto Hero Card
            item(key = "manifesto_hero") {
                Surface(
                    shape = VittifyShapes.hero,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Privacy Manifesto",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "Your money is your business. Vittify runs 100% on your device, contains zero ads, zero trackers, and never uploads your personal data to remote servers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )

                        Text(
                            text = "Last updated: October 2026",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Policy Sections
            items(POLICY_SECTIONS, key = { it.title }) { section ->
                Surface(
                    shape = VittifyShapes.platter,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            shape = VittifyShapes.platter
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        section.points.forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = point,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }

            // Footer assurance
            item(key = "footer_assurance") {
                Text(
                    text = "If you have any questions or feedback regarding our privacy practices, contact us at thegodscode@gmail.com.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.md)
                )
            }
        }
    }
}

