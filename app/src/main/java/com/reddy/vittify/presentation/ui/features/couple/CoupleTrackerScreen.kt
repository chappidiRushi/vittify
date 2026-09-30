package com.reddy.vittify.presentation.ui.features.couple

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.data.sync.transport.ConnectionState
import com.reddy.vittify.data.sync.engine.SyncProgressPhase
import com.reddy.vittify.data.sync.engine.SyncProgressState
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.LocalVittifyTokens
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun getRequiredP2pPermissions(): List<String> {
    val list = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.BLUETOOTH_SCAN)
        list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        list.add(Manifest.permission.BLUETOOTH_CONNECT)
        list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        list.add(Manifest.permission.BLUETOOTH_SCAN)
        list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        list.add(Manifest.permission.BLUETOOTH_CONNECT)
        list.add(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        list.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    return list
}

private fun hasAllP2pPermissions(context: Context): Boolean {
    return getRequiredP2pPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CoupleTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: CoupleTrackerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var permissionsGranted by remember { mutableStateOf(hasAllP2pPermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) {
            viewModel.toggleCoupleTracking(true)
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Partner Sync",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = LocalBottomNavPadding.current)) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.md)
                .padding(bottom = 120.dp + LocalBottomNavPadding.current),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            val tokens = LocalVittifyTokens.current
            // Master Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = VittifyShapes.hero,
                border = VittifySurface.platterBorder(
                    strokeColor = if (uiState.isCoupleTrackingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isCoupleTrackingEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f * tokens.surfaceOpacity)
                    } else {
                        VittifySurface.platterContainerColor()
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isCoupleTrackingEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = if (uiState.isCoupleTrackingEnabled) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.md))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Partner Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isCoupleTrackingEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = if (uiState.isCoupleTrackingEnabled) {
                                "Direct P2P sync enabled"
                            } else {
                                "Track shared finances with your partner"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isCoupleTrackingEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    Switch(
                        checked = uiState.isCoupleTrackingEnabled,
                        onCheckedChange = { enable ->
                            if (enable && !permissionsGranted) {
                                permissionLauncher.launch(getRequiredP2pPermissions().toTypedArray())
                            } else {
                                viewModel.toggleCoupleTracking(enable)
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isCoupleTrackingEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    val isPaired = !uiState.partnerUserId.isNullOrBlank()

                    // Pairing Card
                    if (isPaired) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = VittifyShapes.platter,
                            border = VittifySurface.platterBorder(strokeColor = MaterialTheme.colorScheme.tertiary),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f * tokens.surfaceOpacity)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                verticalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Paired with ${uiState.partnerName ?: "Partner"}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }

                                // Status pills for Local P2P
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    val localStatusText = when {
                                        !uiState.isLocalP2pEnabled -> "Off"
                                        uiState.localConnectionState is ConnectionState.Connected -> "Connected"
                                        uiState.localConnectionState is ConnectionState.Connecting -> "Connecting"
                                        else -> "Disconnected"
                                    }
                                    StatusPill(
                                        label = "Local P2P",
                                        statusText = localStatusText,
                                        state = uiState.localConnectionState,
                                        isEnabled = uiState.isLocalP2pEnabled,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    /*
                                    val remoteStatusText = when {
                                        !uiState.isRemoteP2pEnabled -> "Off"
                                        uiState.remoteConnectionState is ConnectionState.Connected -> "Connected"
                                        uiState.remoteConnectionState is ConnectionState.Connecting -> "Connecting"
                                        else -> "Disconnected"
                                    }
                                    StatusPill(
                                        label = "Remote P2P",
                                        statusText = remoteStatusText,
                                        state = uiState.remoteConnectionState,
                                        isEnabled = uiState.isRemoteP2pEnabled,
                                        modifier = Modifier.weight(1f)
                                    )
                                    */
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)
                                )

                                // Connection and Sync Info
                                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                    val lastConnectedStr = if (uiState.lastConnectedTimestamp > 0L) {
                                        SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
                                            .format(Date(uiState.lastConnectedTimestamp))
                                    } else {
                                        "Never"
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Last connected",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = lastConnectedStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    val lastSyncStr = if (uiState.lastSyncTimestamp > 0L) {
                                        SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
                                            .format(Date(uiState.lastSyncTimestamp))
                                    } else {
                                        "Never"
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Last synced",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = lastSyncStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Changes to sync",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (uiState.pendingChangesCount > 0) {
                                            Surface(
                                                shape = VittifyShapes.small,
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = "${uiState.pendingChangesCount} pending",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "All synced",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                val isAnyConnected = uiState.localConnectionState is ConnectionState.Connected ||
                                    uiState.remoteConnectionState is ConnectionState.Connected

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        if (!isAnyConnected) {
                                            OutlinedButton(
                                                onClick = { viewModel.retryConnection() },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(Spacing.xs))
                                                Text("Try Again", maxLines = 1)
                                            }
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                if (uiState.isSyncing) {
                                                    viewModel.showSyncProgressModal()
                                                } else {
                                                    viewModel.triggerSync()
                                                }
                                            },
                                            enabled = true,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            if (uiState.isSyncing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(Spacing.xs))
                                                Text("Syncing...", maxLines = 1)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Rounded.Sync,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(Spacing.xs))
                                                Text("Sync Now", maxLines = 1)
                                            }
                                        }

                                        if (isAnyConnected) {
                                            OutlinedButton(
                                                onClick = { viewModel.showUnpairDialog() },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LinkOff,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(Spacing.xs))
                                                Text("Unpair", maxLines = 1)
                                            }
                                        }
                                    }

                                    if (!isAnyConnected) {
                                        OutlinedButton(
                                            onClick = { viewModel.showUnpairDialog() },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.LinkOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.xs))
                                            Text("Unpair", maxLines = 1)
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = uiState.isSyncing && !uiState.syncProgress.isVisible,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = Spacing.sm),
                                        shape = VittifyShapes.scaled(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                        onClick = { viewModel.showSyncProgressModal() }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Column {
                                                    Text(
                                                        text = "Sync in progress",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    val statusText = uiState.syncProgress.statusMessage.ifBlank { "Tap to view progress" }
                                                    Text(
                                                        text = statusText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                            ) {
                                                TextButton(
                                                    onClick = { viewModel.cancelSync() },
                                                    colors = ButtonDefaults.textButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    )
                                                ) {
                                                    Text(
                                                        text = "Cancel",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                TextButton(
                                                    onClick = { viewModel.showSyncProgressModal() }
                                                ) {
                                                    Text(
                                                        text = "Maximize",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Not paired yet - Pairing actions
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = VittifyShapes.platter,
                            border = VittifySurface.platterBorder(),
                            colors = CardDefaults.cardColors(
                                containerColor = VittifySurface.platterContainerColor()
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                verticalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                Text(
                                    text = "Connect with Partner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pair both devices to automatically sync and view shared transactions, cards, and budgets.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                uiState.clusterId?.let { cluster ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Your Pairing Code",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = cluster,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Pairing Code", cluster))
                                                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ContentCopy,
                                                    contentDescription = "Copy code",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Button(
                                        onClick = { viewModel.showQrCodeDialog() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text("Show QR")
                                    }

                                    FilledTonalButton(
                                        onClick = { viewModel.scanQrCode(context) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text("Scan QR")
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.showPairCodeDialog() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text("Enter Partner's Code")
                                }
                            }
                        }
                    }

                    // Section: Periodic Background Sync
                    Column {
                        SectionHeader(
                            title = "Background Sync"
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            val nextUtcMillis = ((System.currentTimeMillis() / 3_600_000L) + 1L) * 3_600_000L
                            val utcFormat = SimpleDateFormat("HH:00 'UTC'", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val localFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val subtitleText = if (uiState.isHourlySyncEnabled) {
                                "Next window at ${utcFormat.format(Date(nextUtcMillis))} (${localFormat.format(Date(nextUtcMillis))} local). Both devices attempt connection for 5 minutes."
                            } else {
                                "Sync with partner every hour even when the app is closed. Both devices align to UTC hour and try for 5 minutes."
                            }

                            PreferenceSwitch(
                                title = "Hourly Background Sync (UTC)",
                                subtitle = subtitleText,
                                checked = uiState.isHourlySyncEnabled,
                                onCheckedChange = { viewModel.toggleHourlySync(it) },
                                padding = PaddingValues(0.dp),
                                isSingle = true
                            )
                        }
                    }

                    // Section: My Privacy & Sharing (all 4 default ON)
                    Column {
                        SectionHeader(
                            title = "My Sharing & Privacy"
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            PreferenceSwitch(
                                title = "Share my data with partner",
                                subtitle = "Allow partner to receive transactions and budgets",
                                checked = uiState.shareMyData,
                                onCheckedChange = { viewModel.setShareMyData(it) },
                                padding = PaddingValues(0.dp),
                                isFirst = true
                            )

                            PreferenceSwitch(
                                title = "Partner can edit my data",
                                subtitle = "Allow partner to edit transactions you created",
                                checked = uiState.partnerCanEditMyData,
                                onCheckedChange = { viewModel.setPartnerCanEditMyData(it) },
                                padding = PaddingValues(0.dp)
                            )

                            PreferenceSwitch(
                                title = "Partner can delete my data",
                                subtitle = "Allow partner to delete transactions you created",
                                checked = uiState.partnerCanDeleteMyData,
                                onCheckedChange = { viewModel.setPartnerCanDeleteMyData(it) },
                                padding = PaddingValues(0.dp)
                            )

                            PreferenceSwitch(
                                title = "Partner can see my bank balances",
                                subtitle = "Share real-time account balances and card limits",
                                checked = uiState.partnerCanSeeMyBalances,
                                onCheckedChange = { viewModel.setPartnerCanSeeMyBalances(it) },
                                padding = PaddingValues(0.dp),
                                isLast = true
                            )
                        }
                    }

                    // Section: Partner's Permissions
                    Column {
                        SectionHeader(
                            title = "Partner's Permissions"
                        )

                        if (isPaired) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(1.5.dp)
                            ) {
                                PermissionStatusItem(
                                    title = "Partner shares data",
                                    allowed = uiState.partnerAllowsShareData,
                                    position = ListItemPosition.Top
                                )
                                PermissionStatusItem(
                                    title = "You can edit partner's data",
                                    allowed = uiState.partnerAllowsEditData,
                                    position = ListItemPosition.Middle
                                )
                                PermissionStatusItem(
                                    title = "You can delete partner's data",
                                    allowed = uiState.partnerAllowsDeleteData,
                                    position = ListItemPosition.Middle
                                )
                                PermissionStatusItem(
                                    title = "You can see partner's balances",
                                    allowed = uiState.partnerAllowsSeeBalances,
                                    position = ListItemPosition.Bottom
                                )
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = VittifyShapes.platter,
                                border = VittifySurface.platterBorder(),
                                colors = CardDefaults.cardColors(
                                    containerColor = VittifySurface.platterContainerColor()
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.lg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Partner permissions will be visible here once paired.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // QR Dialog
    if (uiState.showQrDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissQrCodeDialog() },
            title = {
                Text(
                    text = "Pair with Partner",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        text = "Scan this QR code from your partner's device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    val bitmap = uiState.qrBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Pairing QR Code",
                            modifier = Modifier
                                .size(240.dp)
                                .clip(VittifyShapes.hero)
                                .background(Color.White)
                                .padding(12.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(VittifyShapes.hero)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.clusterId?.let { cluster ->
                        Text(
                            text = "Code: $cluster",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissQrCodeDialog() }) {
                    Text("Done")
                }
            }
        )
    }

    // Enter Pairing Code Dialog
    if (uiState.showPairCodeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPairCodeDialog() },
            title = { Text("Enter Partner's Code") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "Enter the pairing code shown on your partner's device:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = uiState.pairingCodeInput,
                        onValueChange = { viewModel.updatePairingCodeInput(it) },
                        placeholder = { Text("e.g. vittify-a8f3b2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyPairingInput(uiState.pairingCodeInput)
                    },
                    enabled = uiState.pairingCodeInput.isNotBlank()
                ) {
                    Text("Pair")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPairCodeDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unpair Dialog
    if (uiState.showUnpairDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnpairDialog() },
            title = { Text("Unpair Partner?") },
            text = {
                Text("Syncing between your devices will be stopped. Existing transactions on this device will remain.")
                Text("Syncing between your devices will be stopped. Partner data will be removed from this device, and your view will switch back to your personal profile. If connected, your data will also be removed from your partner's device.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmUnpair() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Unpair")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUnpairDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Real-Time Sync Progress Modal
    if (uiState.syncProgress.isVisible) {
        SyncProgressDialog(
            progressState = uiState.syncProgress,
            onDismiss = { viewModel.dismissSyncProgressModal() },
            onCancel = { viewModel.cancelSync() }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SyncProgressDialog(
    progressState: SyncProgressState,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (progressState.phase == SyncProgressPhase.COMPLETED || progressState.phase == SyncProgressPhase.ERROR) {
                onDismiss()
            }
        },
        shape = VittifyShapes.dialog,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                when (progressState.phase) {
                    SyncProgressPhase.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    SyncProgressPhase.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = when (progressState.phase) {
                        SyncProgressPhase.COMPLETED -> "Sync Complete"
                        SyncProgressPhase.ERROR -> "Sync Interrupted"
                        SyncProgressPhase.SENDING -> "Sending Data"
                        SyncProgressPhase.RECEIVING -> "Receiving Data"
                        SyncProgressPhase.PREPARING -> "Preparing Sync"
                        SyncProgressPhase.CONNECTING -> "Connecting"
                        else -> "Syncing"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                if (progressState.partnerDeviceName.isNotBlank()) {
                    Text(
                        text = "Partner: ${progressState.partnerDeviceName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (progressState.phase == SyncProgressPhase.ERROR) {
                        progressState.errorMessage ?: progressState.statusMessage
                    } else {
                        progressState.statusMessage
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (progressState.phase == SyncProgressPhase.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                when (progressState.phase) {
                    SyncProgressPhase.SENDING, SyncProgressPhase.RECEIVING -> {
                        if (progressState.total > 0) {
                            val progressFraction = (progressState.current.toFloat() / progressState.total.toFloat()).coerceIn(0f, 1f)
                            LinearWavyProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(VittifyShapes.pill)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${progressState.current} / ${progressState.total}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            LinearWavyProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(VittifyShapes.pill)
                            )
                        }
                    }
                    SyncProgressPhase.PREPARING, SyncProgressPhase.CONNECTING -> {
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(VittifyShapes.pill)
                        )
                    }
                    SyncProgressPhase.COMPLETED -> {
                        LinearWavyProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(VittifyShapes.pill),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {}
                }

                if (progressState.sentCount > 0 || progressState.receivedCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(VittifyShapes.scaled(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(
                            text = "Sent: ${progressState.sentCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Received: ${progressState.receivedCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (progressState.phase == SyncProgressPhase.COMPLETED) {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            } else if (progressState.phase == SyncProgressPhase.ERROR) {
                Button(onClick = onDismiss) {
                    Text("Dismiss")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Minimize")
                }
            }
        },
        dismissButton = {
            if (progressState.phase != SyncProgressPhase.COMPLETED && progressState.phase != SyncProgressPhase.ERROR) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel Sync")
                }
            }
        }
    )
}

@Composable
private fun PermissionStatusItem(
    title: String,
    allowed: Boolean,
    position: ListItemPosition
) {
    ListItem(
        headline = { Text(title) },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    imageVector = if (allowed) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (allowed) "Allowed" else "Restricted",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        },
        padding = PaddingValues(0.dp),
        shape = position.toShape()
    )
}

@Composable
private fun StatusPill(
    label: String,
    statusText: String,
    state: ConnectionState,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val (bgColor, contentColor, dotColor) = when {
        !isEnabled -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline
        )
        state is ConnectionState.Connected -> if (isDark) {
            Triple(Color(0xFF1B3D23), Color(0xFF81C784), Color(0xFF4CAF50))
        } else {
            Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFF2E7D32))
        }
        state is ConnectionState.Connecting -> if (isDark) {
            Triple(Color(0xFF3D2D1B), Color(0xFFFFB74D), Color(0xFFFF9800))
        } else {
            Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFF57C00))
        }
        else -> if (isDark) {
            Triple(Color(0xFF3D1B1B), Color(0xFFE57373), Color(0xFFEF5350))
        } else {
            Triple(Color(0xFFFFEBEE), Color(0xFFB71C1C), Color(0xFFD32F2F))
        }
    }

    Surface(
        shape = VittifyShapes.scaled(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = "$label: $statusText",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

