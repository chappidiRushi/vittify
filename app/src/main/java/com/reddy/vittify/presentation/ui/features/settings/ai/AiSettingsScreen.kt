package com.reddy.vittify.presentation.ui.features.settings.ai

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reddy.vittify.R
import com.reddy.vittify.data.ai.ConnectionTestState
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.navigation.SettingsDeepLink
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.settingOptionHighlight
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.SecuritySafe
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.green_dark
import com.reddy.vittify.presentation.ui.theme.green_light
import com.reddy.vittify.presentation.ui.theme.purple_dark
import com.reddy.vittify.presentation.ui.theme.purple_light
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.relocation.BringIntoViewRequester

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiSettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    targetOptionId: String? = null,
    viewModel: AiSettingsViewModel = hiltViewModel(),
    blurEffects: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val bringIntoViewRequesters = remember {
        mapOf(
            "enable-ai" to BringIntoViewRequester(),
            "api-key" to BringIntoViewRequester(),
            "ai-model" to BringIntoViewRequester()
        )
    }
    var highlightedOptionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetOptionId) {
        targetOptionId?.let { rawId ->
            val id = SettingsDeepLink.normalizeOptionSlug("ai", rawId)
            if (id in listOf("api-key", "ai-model") && !uiState.isAiEnabled) {
                viewModel.onToggleAiEnabled(true)
            }
            delay(350)
            bringIntoViewRequesters[id]?.bringIntoView()
            highlightedOptionId = id
            delay(2500)
            highlightedOptionId = null
        }
    }

    var showApiKeyVisible by remember { mutableStateOf(false) }
    var isModelMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.ai_integration_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        }
    ) { paddingValues ->
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
                    bottom = Dimensions.Padding.content + 60.dp
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // ─────────────────────────────────────────────────────────────
            // 1. Top Disclosure Card (Vittify Intelligence)
            // ─────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                shape = VittifyShapes.hero
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.vittify_intelligence),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.onToggleOnlineModelDialog(true)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = stringResource(R.string.online_model),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.vittify_intelligence_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(VittifyShapes.small)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.onToggleGuideDialog(true)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.view_setup_guide),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 2. Google Gemini Main Configuration Platter
            // ─────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
                    .settingOptionHighlight(
                        id = "enable-ai",
                        requester = bringIntoViewRequesters["enable-ai"],
                        highlightedId = highlightedOptionId,
                        shape = VittifyShapes.hero
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                shape = VittifyShapes.hero
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row with Toggle & Info Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(purple_light, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = purple_dark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.google_gemini),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            viewModel.onToggleOnlineModelDialog(true)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Info,
                                            contentDescription = stringResource(R.string.online_model),
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.online_model),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isAiEnabled,
                            onCheckedChange = { checked ->
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.onToggleAiEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }

                    // Content Visible Only When Master Toggle is ON
                    AnimatedVisibility(
                        visible = uiState.isAiEnabled,
                        enter = fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )

                            // Missing API Key Callout Banner
                            if (uiState.geminiConfig.apiKey.isBlank()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(VittifyShapes.medium)
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                            context.startActivity(intent)
                                        },
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                                    shape = VittifyShapes.medium,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.missing_api_key),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = stringResource(R.string.missing_api_key_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }

                            // API Key Input Field
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .settingOptionHighlight(
                                        id = "api-key",
                                        requester = bringIntoViewRequesters["api-key"],
                                        highlightedId = highlightedOptionId,
                                        shape = VittifyShapes.input
                                    ),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.api_key_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                OutlinedTextField(
                                    value = uiState.geminiConfig.apiKey,
                                    onValueChange = { viewModel.onApiKeyChanged(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(text = stringResource(R.string.api_key_hint))
                                    },
                                    singleLine = true,
                                    visualTransformation = if (showApiKeyVisible) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Key,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (uiState.geminiConfig.apiKey.isNotEmpty()) {
                                                IconButton(onClick = { viewModel.onApiKeyChanged("") }) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Clear,
                                                        contentDescription = stringResource(R.string.clear_key)
                                                    )
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.getText()?.text?.let { text ->
                                                            viewModel.onApiKeyChanged(text)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.ContentPaste,
                                                        contentDescription = stringResource(R.string.paste_from_clipboard),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { showApiKeyVisible = !showApiKeyVisible }) {
                                                Icon(
                                                    imageVector = if (showApiKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        keyboardController?.hide()
                                        viewModel.onFetchModels()
                                    }),
                                    shape = VittifyShapes.input
                                )
                            }

                            // Get API Key Row Link
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VittifyShapes.small)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                        context.startActivity(intent)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Key,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.get_api_key),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )

                            // AI Model Selector Dropdown Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .settingOptionHighlight(
                                        id = "ai-model",
                                        requester = bringIntoViewRequesters["ai-model"],
                                        highlightedId = highlightedOptionId,
                                        shape = VittifyShapes.medium
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Memory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.model_selection),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (uiState.isFetchingModels) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = VittifyShapes.pill
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Loading…",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else if (uiState.geminiConfig.availableModels.isEmpty()) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(VittifyShapes.pill)
                                                .clickable {
                                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                    viewModel.onFetchModels()
                                                },
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = VittifyShapes.pill
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = if (uiState.geminiConfig.apiKey.isBlank()) {
                                                        stringResource(R.string.select_model)
                                                    } else {
                                                        "Fetch Models"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .clip(VittifyShapes.pill)
                                                .clickable {
                                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                    isModelMenuExpanded = true
                                                },
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = VittifyShapes.pill
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = uiState.geminiConfig.selectedModel.ifBlank { stringResource(R.string.select_model) },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.widthIn(max = 160.dp)
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = isModelMenuExpanded,
                                            onDismissRequest = { isModelMenuExpanded = false },
                                            modifier = Modifier.heightIn(max = 350.dp)
                                        ) {
                                            uiState.geminiConfig.availableModels.forEach { model ->
                                                val isSelected = uiState.geminiConfig.selectedModel == model
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = model,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    },
                                                    leadingIcon = if (isSelected) {
                                                        {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    } else null,
                                                    onClick = {
                                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                        viewModel.onModelSelected(model)
                                                        isModelMenuExpanded = false
                                                    }
                                                )
                                            }
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = stringResource(R.string.refresh_model_list),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Refresh,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
                                                onClick = {
                                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                    viewModel.onFetchModels()
                                                    isModelMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Test Connection Button
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    viewModel.onTestConnection()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.testState !is ConnectionTestState.Testing,
                                shape = VittifyShapes.button,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (uiState.testState is ConnectionTestState.Testing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Testing Connection...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = stringResource(R.string.test_connection_btn))
                                }
                            }

                            // Connection Test Banner
                            when (val state = uiState.testState) {
                                is ConnectionTestState.Success -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(VittifyShapes.medium)
                                            .background(green_light)
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = green_dark
                                            )
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = green_dark,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                is ConnectionTestState.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(VittifyShapes.medium)
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 3. Quick Add Options Section Card (Intelligence Settings)
            // ─────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                shape = VittifyShapes.hero
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quick_add_options),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Option A: Include Categories and Subcategories
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.include_categories),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.include_categories_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.geminiConfig.includeCategories,
                            onCheckedChange = { checked ->
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.onToggleIncludeCategories(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Option B: Include Bank Accounts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.include_bank_accounts),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.include_bank_accounts_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.geminiConfig.includeBankAccounts,
                            onCheckedChange = { checked ->
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.onToggleIncludeBankAccounts(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Extra Rules / Custom Instructions Input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.extra_rules_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = uiState.geminiConfig.customRules,
                            onValueChange = { viewModel.onCustomRulesChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(text = stringResource(R.string.extra_rules_hint))
                            },
                            minLines = 2,
                            maxLines = 4,
                            shape = VittifyShapes.input
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 4. Usage Metrics Section
            // ─────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                shape = VittifyShapes.hero
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.usage_metrics),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.onResetMetrics()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = VittifyShapes.button
                        ) {
                            Text(
                                text = stringResource(R.string.reset_metrics),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.total_requests),
                            value = uiState.geminiConfig.totalRequests.toString(),
                            icon = Icons.Filled.AutoAwesome
                        )
                        MetricStatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.total_tokens),
                            value = formatTokenCount(uiState.geminiConfig.totalTokens),
                            icon = Icons.Rounded.Info
                        )
                    }

                    if (uiState.geminiConfig.lastTestedAt > 0L) {
                        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
                        Text(
                            text = "Last verified: ${dateFormat.format(Date(uiState.geminiConfig.lastTestedAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 5. Online Model Disclosure Modal Dialog
        // ─────────────────────────────────────────────────────────────
        if (uiState.showOnlineModelDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onToggleOnlineModelDialog(false) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(purple_light, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Iconax.SecuritySafe,
                            contentDescription = null,
                            tint = purple_dark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.online_model_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.online_model_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            viewModel.onToggleOnlineModelDialog(false)
                        },
                        shape = VittifyShapes.button,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            viewModel.onToggleOnlineModelDialog(false)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ai.google.dev/terms"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.google_gemini_terms))
                    }
                },
                shape = VittifyShapes.dialog
            )
        }

        // ─────────────────────────────────────────────────────────────
        // 6. API Key Setup Guide Dialog
        // ─────────────────────────────────────────────────────────────
        if (uiState.showGuideDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onToggleGuideDialog(false) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.api_key_guide_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = stringResource(R.string.api_key_guide_step1), style = MaterialTheme.typography.bodyMedium)
                        Text(text = stringResource(R.string.api_key_guide_step2), style = MaterialTheme.typography.bodyMedium)
                        Text(text = stringResource(R.string.api_key_guide_step3), style = MaterialTheme.typography.bodyMedium)
                        Text(text = stringResource(R.string.api_key_guide_step4), style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        },
                        shape = VittifyShapes.button
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.open_google_ai_studio))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onToggleGuideDialog(false) }) {
                        Text(text = "Close")
                    }
                },
                shape = VittifyShapes.dialog
            )
        }
    }
}

@Composable
private fun MetricStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = modifier
            .clip(VittifyShapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)), VittifyShapes.medium)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatTokenCount(tokens: Long): String {
    return when {
        tokens >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", tokens / 1_000_000.0)
        tokens >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", tokens / 1_000.0)
        else -> tokens.toString()
    }
}
