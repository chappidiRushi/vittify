package com.reddy.vittify.presentation.ui.features.settings.developer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.features.settings.SettingsViewModel
import com.reddy.vittify.presentation.ui.icons.Glass
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.NotificationBing
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.grey_dark
import com.reddy.vittify.presentation.ui.theme.grey_light
import androidx.compose.material.icons.rounded.Webhook
import androidx.compose.material.icons.rounded.Code
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val isWebhookModeEnabled by
            settingsViewModel.isWebhookModeEnabled.collectAsStateWithLifecycle(
                initialValue = false
            )


    val isTestNotificationAlertsEnabled by
        settingsViewModel.isTestNotificationAlertsEnabled.collectAsStateWithLifecycle(
            initialValue = false
        )

    val isSampleDataSeeded by
        settingsViewModel.isSampleDataSeeded.collectAsStateWithLifecycle(
            initialValue = false
        )

    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val bottomNavPadding = LocalBottomNavPadding.current

    var isSimulating by remember { mutableStateOf(false) }
    var pendingSimulation by remember { mutableStateOf<Pair<String, String>?>(null) }

    val executeSimulation: (String, String) -> Unit = { sender, body ->
        isSimulating = true
        scope.launch {
            try {
                val result = settingsViewModel.simulateSmsTransaction(sender, body)
                if (result.success) {
                    snackbarHostState.showSnackbar("Processed transaction #${result.transactionId}")
                } else if (result.reason == "Duplicate transaction") {
                    snackbarHostState.showSnackbar("Transaction already exists, notification posted")
                } else {
                    snackbarHostState.showSnackbar(result.reason ?: "Could not parse SMS")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isSimulating = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingSimulation?.let { (sender, body) ->
                pendingSimulation = null
                executeSimulation(sender, body)
            }
        } else {
            pendingSimulation = null
            scope.launch {
                snackbarHostState.showSnackbar("Notification permission is required to display transaction alerts")
            }
        }
    }

    LaunchedEffect(uiState.seedMessage) {
        uiState.seedMessage?.let {
            snackbarHostState.showSnackbar(it)
            settingsViewModel.clearSeedMessage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
                CustomTitleTopAppBar(
                title = stringResource(R.string.developer_options),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = bottomNavPadding)
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                        bottom = Dimensions.Padding.content + paddingValues.calculateBottomPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                PreferenceSwitch(
                    title = stringResource(R.string.webhook_mode),
                    subtitle = stringResource(R.string.webhook_mode_desc),
                    checked = isWebhookModeEnabled,
                    onCheckedChange = { settingsViewModel.toggleWebhookMode(it) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = grey_light,
                                    shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.SCALLOP_12.composeShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Webhook,
                                contentDescription = null,
                                tint = grey_dark
                            )
                        }
                    },
                    padding = PaddingValues(0.dp),
                    isSingle = false,
                    isFirst = true
                )
                
                PreferenceSwitch(
                    title = stringResource(R.string.test_notification_alerts),
                    subtitle = stringResource(R.string.test_notification_alerts_desc),
                    checked = isTestNotificationAlertsEnabled,
                    onCheckedChange = { settingsViewModel.toggleTestNotificationAlerts(it) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = grey_light,
                                    shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.COOKIE_8.composeShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Iconax.NotificationBing,
                                contentDescription = null,
                                tint = grey_dark
                            )
                        }
                    },
                    padding = PaddingValues(0.dp),
                    isSingle = false,
                    isLast = false
                )

                var showRandomConfigSheet by remember { mutableStateOf(false) }

                PreferenceSwitch(
                    title = stringResource(R.string.sample_data),
                    subtitle = "Generate configurable 2-year random financial history with verified balances",
                    checked = isSampleDataSeeded,
                    onCheckedChange = { wantEnabled ->
                        if (!uiState.isSeeding) {
                            if (wantEnabled) {
                                showRandomConfigSheet = true
                            } else {
                                settingsViewModel.toggleSampleData(false)
                            }
                        }
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = grey_light,
                                    shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.PENTAGON.composeShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isSeeding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = grey_dark
                                )
                            } else {
                                Icon(
                                    Iconax.Glass,
                                    contentDescription = null,
                                    tint = grey_dark
                                )
                            }
                        }
                    },
                    padding = PaddingValues(0.dp),
                    isSingle = false,
                    isLast = true
                )

                if (isSampleDataSeeded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = { if (!uiState.isSeeding) showRandomConfigSheet = true },
                        enabled = !uiState.isSeeding,
                        modifier = Modifier.fillMaxWidth(),
                        shape = VittifyShapes.button
                    ) {
                        Text("Configure & Regenerate Random Data")
                    }
                }

                if (showRandomConfigSheet) {
                    com.reddy.vittify.presentation.ui.components.RandomDataConfigSheet(
                        onDismiss = { showRandomConfigSheet = false },
                        onGenerate = { config ->
                            showRandomConfigSheet = false
                            settingsViewModel.seedSampleData(config)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulate SMS Transaction Card
                var simSender by remember { mutableStateOf("HDFCBK") }
                var simBody by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = VittifyShapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = VittifySurface.surfaceContainerLowColor()
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.simulate_sms_transaction),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.simulate_sms_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = simSender,
                            onValueChange = { simSender = it },
                            label = { Text(stringResource(R.string.sender_address)) },
                            singleLine = true,
                            shape = VittifyShapes.input,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = simBody,
                            onValueChange = { simBody = it },
                            label = { Text(stringResource(R.string.sample_message)) },
                            placeholder = { Text("e.g. Sent Rs.450.00 from HDFC Bank AC **1234 to SWIGGY on 15-09-24") },
                            minLines = 3,
                            maxLines = 5,
                            shape = VittifyShapes.input,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (simBody.isNotBlank() && !isSimulating) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        pendingSimulation = Pair(simSender, simBody)
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        executeSimulation(simSender, simBody)
                                    }
                                }
                            },
                            enabled = simBody.isNotBlank() && !isSimulating,
                            modifier = Modifier.fillMaxWidth(),
                            shape = VittifyShapes.button
                        ) {
                            if (isSimulating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.simulate_button))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(110.dp + bottomNavPadding + 32.dp))
            }
        }
    }
}
