package com.reddy.vittify.presentation.ui.features.onboarding.steps

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.R
import com.reddy.vittify.data.cloud.CloudFileInfo
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Step 3: Data Recovery (Cloud Online & Local Backup).
 * Allows users to log in to Google Drive, browse snapshots, restore local backups,
 * or skip to manual setup.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DataRecoveryStep(
    isGoogleDriveSignedIn: Boolean,
    googleDriveEmail: String?,
    remoteSnapshots: List<CloudFileInfo>,
    isLoadingSnapshots: Boolean,
    isRestoring: Boolean,
    restoreProgress: Int,
    restoreStatusMessage: String?,
    showPassphraseDialog: Boolean,
    passphraseInput: String,
    onPassphraseChange: (String) -> Unit,
    onConfirmPassphrase: () -> Unit,
    onDismissPassphraseDialog: () -> Unit,
    onSignInGoogleDrive: () -> Unit,
    onSignOutGoogleDrive: () -> Unit,
    onRefreshSnapshots: () -> Unit,
    onRestoreSnapshot: (CloudFileInfo) -> Unit,
    onRestoreLocalBackup: (Uri) -> Unit,
    isGeneratingSampleData: Boolean = false,
    sampleDataProgress: Int = 0,
    sampleDataStatusMessage: String? = null,
    isSampleDataSeeded: Boolean = false,
    onGenerateRandomData: (com.reddy.vittify.data.generator.RandomDataConfig) -> Unit = {},
    onClearRandomData: () -> Unit = {},
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onRestoreLocalBackup(it) }
    }

    var showRandomDataConfigSheet by remember { mutableStateOf(false) }
    val isBusy = isRestoring || isGeneratingSampleData

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.data_recovery_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.data_recovery_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Restore or Sample Data Generation Progress Banner
        AnimatedVisibility(
            visible = isBusy,
            enter = fadeIn() + androidx.compose.animation.expandVertically(),
            exit = fadeOut() + androidx.compose.animation.shrinkVertically()
        ) {
            val activeProgress = if (isGeneratingSampleData) sampleDataProgress else restoreProgress
            val activeMessage = if (isGeneratingSampleData) {
                sampleDataStatusMessage ?: "Generating 2-year random financial history..."
            } else {
                restoreStatusMessage ?: stringResource(R.string.restoring_progress_format, restoreProgress)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = VittifyShapes.hero,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = activeMessage,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    LinearWavyProgressIndicator(
                        progress = { activeProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // SECTION 1: Cloud Restore Platter
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(spring(stiffness = Spring.StiffnessMedium)),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.surfaceContainerLowColor()
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.restore_online_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.restore_online_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isGoogleDriveSignedIn) {
                        IconButton(
                            onClick = onRefreshSnapshots,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.refresh),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (!isGoogleDriveSignedIn) {
                    // Google Drive Sign-In CTA
                    Button(
                        onClick = onSignInGoogleDrive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = VittifyShapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_brand_google_drive),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.sign_in_with_google_drive),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Signed In Account Status
                    Surface(
                        shape = VittifyShapes.large,
                        color = VittifySurface.surfaceContainerHighColor(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.signed_in),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = googleDriveEmail ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = onSignOutGoogleDrive) {
                                Text(
                                    text = stringResource(R.string.sign_out),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // Remote Snapshots List
                    if (isLoadingSnapshots) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.md),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    } else if (remoteSnapshots.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_cloud_backups_found),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.xs)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.available_cloud_backups),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            remoteSnapshots.forEach { snapshot ->
                                RemoteSnapshotItemRow(
                                    snapshot = snapshot,
                                    isRestoring = isRestoring,
                                    onRestoreClick = { onRestoreSnapshot(snapshot) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: Local File Restore Platter
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.surfaceContainerLowColor()
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.restore_local_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.restore_local_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("application/zip", "application/json", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = VittifyShapes.button,
                    enabled = !isRestoring
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(R.string.browse_backup_file),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // SECTION 3: Generate Random Sample Data (2-Year Realistic Financial History)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.surfaceContainerLowColor()
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Explore with Random Test Data",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Generate 2 years of realistic accounts, multi-currency history, budgets & verified balances",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showRandomDataConfigSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = VittifyShapes.button,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(
                        text = if (isSampleDataSeeded) "Regenerate Random Data (Configure)" else "Generate Random Data",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isSampleDataSeeded) {
                    TextButton(
                        onClick = onClearRandomData,
                        enabled = !isBusy,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Clear Sample Data",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // Skip / Set Up Fresh action
        TactilePrimaryButton(
            text = stringResource(R.string.skip_restore_action),
            onClick = onSkip,
            enabled = !isBusy
        )

        Spacer(modifier = Modifier.height(Spacing.xs))
    }

    if (showRandomDataConfigSheet) {
        com.reddy.vittify.presentation.ui.components.RandomDataConfigSheet(
            onDismiss = { showRandomDataConfigSheet = false },
            onGenerate = { config ->
                showRandomDataConfigSheet = false
                onGenerateRandomData(config)
            }
        )
    }

    // Decrypt Passphrase Dialog
    if (showPassphraseDialog) {
        AlertDialog(
            onDismissRequest = onDismissPassphraseDialog,
            shape = VittifyShapes.hero,
            title = {
                Text(
                    text = stringResource(R.string.enter_decrypt_passphrase),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = stringResource(R.string.encrypted_backup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = passphraseInput,
                        onValueChange = onPassphraseChange,
                        label = { Text(stringResource(R.string.passphrase_hint)) },
                        singleLine = true,
                        shape = VittifyShapes.input,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmPassphrase,
                    enabled = passphraseInput.isNotBlank(),
                    shape = VittifyShapes.pill
                ) {
                    Text(stringResource(R.string.decrypt_and_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPassphraseDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun RemoteSnapshotItemRow(
    snapshot: CloudFileInfo,
    isRestoring: Boolean,
    onRestoreClick: () -> Unit
) {
    val isEncrypted = snapshot.name.endsWith(".enc")
    val formattedDate = remember(snapshot.lastModified) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(snapshot.lastModified))
    }
    val formattedSize = remember(snapshot.size) {
        val kb = snapshot.size / 1024
        if (kb > 1024) String.format(Locale.getDefault(), "%.1f MB", kb / 1024f)
        else "$kb KB"
    }

    Surface(
        shape = VittifyShapes.large,
        color = VittifySurface.surfaceContainerHighColor(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isEncrypted) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = stringResource(R.string.encrypted),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "$formattedSize • ${snapshot.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onRestoreClick,
                enabled = !isRestoring,
                shape = VittifyShapes.pill,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.restore_action),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
