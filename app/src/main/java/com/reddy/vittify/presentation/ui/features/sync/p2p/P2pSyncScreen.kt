package com.reddy.vittify.presentation.ui.features.sync.p2p

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.data.sync.transport.ConnectionState
import com.reddy.vittify.data.sync.transport.P2pTransportType
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.theme.Spacing
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2pSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: P2pSyncViewModel = hiltViewModel()
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
            viewModel.toggleSync(true)
        }
    }

    LaunchedEffect(uiState.statusBanner) {
        uiState.statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device-to-Device Sync",
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Master Switch
            PreferenceSwitch(
                title = "Peer-to-Peer Sync",
                subtitle = "Sync directly between devices without cloud servers",
                checked = uiState.isSyncEnabled,
                onCheckedChange = { checked ->
                    if (checked && uiState.isLocalP2pEnabled && !permissionsGranted) {
                        permissionLauncher.launch(getRequiredP2pPermissions().toTypedArray())
                    }
                    viewModel.toggleSync(checked)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isSingle = true
            )

            // Transport Methods
            SectionHeader(title = "Transport Methods")
            Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                PreferenceSwitch(
                    title = "Local Direct P2P",
                    subtitle = "Wi-Fi Direct & Bluetooth (Zero internet required)",
                    checked = uiState.isLocalP2pEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !permissionsGranted) {
                            permissionLauncher.launch(getRequiredP2pPermissions().toTypedArray())
                        }
                        viewModel.toggleLocalP2p(checked)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isFirst = true,
                    isLast = true
                )
                /*
                PreferenceSwitch(
                    title = "Remote P2P (WebRTC)",
                    subtitle = "Direct encrypted sync across internet & mobile data",
                    checked = uiState.isRemoteP2pEnabled,
                    onCheckedChange = { checked ->
                        viewModel.toggleRemoteP2p(checked)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    isLast = true
                )
                */
            }

            // Permission Warning Card if not granted (only needed if local P2P is enabled)
            if (!permissionsGranted && uiState.isSyncEnabled && uiState.isLocalP2pEnabled) {
                Card(
                    shape = VittifyShapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = "Permissions Required for Local Direct P2P",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Grant Bluetooth and Nearby Device permissions so Vittify can discover and sync directly with other devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Button(
                            onClick = { permissionLauncher.launch(getRequiredP2pPermissions().toTypedArray()) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.isSyncEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    // Status Overview Card with Local & Remote Badges
                    StatusOverviewCard(
                        isLocalEnabled = uiState.isLocalP2pEnabled,
                        isRemoteEnabled = uiState.isRemoteP2pEnabled,
                        localConnectionState = uiState.localConnectionState,
                        remoteConnectionState = uiState.remoteConnectionState,
                        lastSyncTime = uiState.lastSyncTimestamp,
                        pendingChanges = uiState.pendingChangesCount,
                        onSyncNow = { viewModel.triggerManualSync() },
                        onRetry = { viewModel.retryConnection() }
                    )

                    // This Device Card
                    SectionHeader(title = "This Device")
                    Surface(
                        shape = VittifyShapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
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
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = uiState.deviceName.ifBlank { "My Android Device" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "ID: ${uiState.deviceId.take(8)}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = {
                                viewModel.openRenameDialog(targetDeviceId = null, initialName = uiState.deviceName)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename this device",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Pairing Section
                    SectionHeader(title = "Pairing & Link")

                    Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                        ListItem(
                            headline = { Text("Show My Pairing QR") },
                            supporting = { Text("Display QR code for other devices to scan") },
                            leading = {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = ListItemPosition.Top.toShape(),
                            onClick = {
                                if (uiState.isLocalP2pEnabled && !permissionsGranted) {
                                    permissionLauncher.launch(getRequiredP2pPermissions().toTypedArray())
                                }
                                viewModel.showQrCodeDialog()
                            }
                        )

                        ListItem(
                            headline = { Text("Scan QR Code") },
                            supporting = { Text("Scan pairing QR code on another device") },
                            leading = {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = ListItemPosition.Middle.toShape(),
                            onClick = {
                                if (uiState.isLocalP2pEnabled && !permissionsGranted) {
                                    permissionLauncher.launch(getRequiredP2pPermissions().toTypedArray())
                                }
                                viewModel.scanQrCode(context)
                            }
                        )

                        ListItem(
                            headline = { Text("Enter Code Manually") },
                            supporting = { Text("Join sync group using a text code") },
                            leading = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = ListItemPosition.Bottom.toShape(),
                            onClick = { viewModel.showPairCodeDialog() }
                        )
                    }

                    // Paired Devices List
                    SectionHeader(title = "Paired Devices (${uiState.pairedDevices.size})")

                    if (uiState.pairedDevices.isEmpty()) {
                        Surface(
                            shape = VittifyShapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    text = "No other devices linked yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap 'Show My Pairing QR' or 'Scan QR Code' to link your other phone.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                            uiState.pairedDevices.forEachIndexed { index, device ->
                                val position = when {
                                    uiState.pairedDevices.size == 1 -> ListItemPosition.Single
                                    index == 0 -> ListItemPosition.Top
                                    index == uiState.pairedDevices.lastIndex -> ListItemPosition.Bottom
                                    else -> ListItemPosition.Middle
                                }
                                val lastSeenStr = if (device.lastSeen > 0) {
                                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(device.lastSeen))
                                } else "Unknown"

                                ListItem(
                                    headline = { Text(device.displayName) },
                                    supporting = {
                                        val subText = if (!device.customNickname.isNullOrBlank()) {
                                            "Name: ${device.deviceName} • Last active: $lastSeenStr"
                                        } else {
                                            "Last active: $lastSeenStr"
                                        }
                                        Text(subText)
                                    },
                                    leading = {
                                        Icon(
                                            imageVector = Icons.Default.Devices,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    trailing = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = {
                                                viewModel.openRenameDialog(
                                                    targetDeviceId = device.deviceId,
                                                    initialName = device.displayName
                                                )
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Rename device",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { viewModel.unpairAndReset() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove device",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    shape = position.toShape(),
                                    onClick = {}
                                )
                            }
                        }
                    }

                    // Reset Cluster Action
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedButton(
                        onClick = { viewModel.unpairAndReset() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Sync Group & Unpair All")
                    }
                }
            }
        }
    }

    // QR Code Dialog
    if (uiState.showQrDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissQrCodeDialog() },
            title = {
                Text(
                    text = "Pair New Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan this QR code from Vittify on your second device:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Keep this screen open. On Device 2, tap 'Scan QR Code' to join.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    uiState.qrBitmap?.let { bitmap ->
                        Surface(
                            shape = VittifyShapes.hero,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp,
                            modifier = Modifier.padding(Spacing.sm)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Pairing QR Code",
                                modifier = Modifier
                                    .size(240.dp)
                                    .padding(8.dp)
                            )
                        }
                    } ?: CircularProgressIndicator(modifier = Modifier.size(48.dp))

                    Spacer(modifier = Modifier.height(Spacing.sm))
                    uiState.clusterId?.let { clusterId ->
                        Text(
                            text = "Sync Code: $clusterId",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
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

    // Manual Code Entry Dialog
    if (uiState.showPairCodeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPairCodeDialog() },
            title = {
                Text(
                    text = "Enter Pairing Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "Enter the Sync Code displayed on your first device:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = uiState.pairingCodeInput,
                        onValueChange = { viewModel.updatePairingCodeInput(it) },
                        placeholder = { Text("e.g. vittify-8f9b1c2d") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.applyPairingInput(uiState.pairingCodeInput) },
                    enabled = uiState.pairingCodeInput.isNotBlank()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPairCodeDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Device Dialog (for My Device or a Paired Device)
    if (uiState.showRenameDialog) {
        var nameInput by remember(uiState.renameTargetInitialName) {
            mutableStateOf(uiState.renameTargetInitialName)
        }
        val isRenamingSelf = uiState.renameTargetDeviceId == null
        AlertDialog(
            onDismissRequest = { viewModel.dismissRenameDialog() },
            title = {
                Text(
                    text = if (isRenamingSelf) "Rename My Device" else "Rename Connected Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = if (isRenamingSelf)
                            "Enter a display name so other devices can recognize your phone:"
                        else
                            "Assign a friendly nickname for this connected device:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("e.g. Living Room Tablet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRename(nameInput) },
                    enabled = nameInput.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRenameDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusOverviewCard(
    isLocalEnabled: Boolean,
    isRemoteEnabled: Boolean,
    localConnectionState: ConnectionState,
    remoteConnectionState: ConnectionState,
    lastSyncTime: Long,
    pendingChanges: Int,
    onSyncNow: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        shape = VittifyShapes.platter,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Live Connection Status Badges & Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    // Local Status Badge (Green / Red Background)
                    val isLocalConnected = localConnectionState is ConnectionState.Connected
                    val localBg = when {
                        !isLocalEnabled -> Color(0xFF616161)
                        isLocalConnected -> Color(0xFF2E7D32) // High-contrast green
                        else -> Color(0xFFC62828) // High-contrast red
                    }
                    val localLabel = when {
                        !isLocalEnabled -> "Local: Off"
                        isLocalConnected -> "Local: Connected"
                        localConnectionState is ConnectionState.Searching -> "Local: Searching"
                        localConnectionState is ConnectionState.Connecting -> "Local: Connecting"
                        else -> "Local: Disconnected"
                    }
                    Surface(
                        shape = VittifyShapes.small,
                        color = localBg
                    ) {
                        Text(
                            text = localLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    /*
                    // Remote Status Badge (Green / Red Background)
                    val isRemoteConnected = remoteConnectionState is ConnectionState.Connected
                    val remoteBg = when {
                        !isRemoteEnabled -> Color(0xFF616161)
                        isRemoteConnected -> Color(0xFF2E7D32) // High-contrast green
                        else -> Color(0xFFC62828) // High-contrast red
                    }
                    val remoteLabel = when {
                        !isRemoteEnabled -> "Remote: Off"
                        isRemoteConnected -> "Remote: Connected"
                        remoteConnectionState is ConnectionState.Searching -> "Remote: Waiting"
                        remoteConnectionState is ConnectionState.Connecting -> "Remote: Connecting"
                        else -> "Remote: Disconnected"
                    }
                    Surface(
                        shape = VittifyShapes.small,
                        color = remoteBg
                    ) {
                        Text(
                            text = remoteLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    */
                }

                // Action Buttons (Try Again + Sync)
                val isAnyConnected = localConnectionState is ConnectionState.Connected || remoteConnectionState is ConnectionState.Connected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isAnyConnected) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = VittifyShapes.button,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Try Again",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Try Again", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = onSyncNow,
                        shape = VittifyShapes.button,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Sync Stats
            val lastSyncStr = if (lastSyncTime > 0) {
                SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(lastSyncTime))
            } else "Never"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Last synced: $lastSyncStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pendingChanges > 0) {
                    Text(
                        text = "$pendingChanges unsynced changes",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}
