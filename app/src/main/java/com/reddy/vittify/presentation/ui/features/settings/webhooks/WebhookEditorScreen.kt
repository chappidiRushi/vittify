package com.reddy.vittify.presentation.ui.features.settings.webhooks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.reddy.vittify.data.database.entity.WebhookDataType
import com.reddy.vittify.data.database.entity.WebhookRangePreset
import com.reddy.vittify.data.webhook.WebhookHeader
import com.reddy.vittify.data.webhook.WebhookProfileDraft
import com.reddy.vittify.data.webhook.WebhookValidation
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.DatePicker
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.icons.Bag
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun WebhookEditorScreen(
    profileId: String?,
    onNavigateBack: () -> Unit,
    viewModel: WebhooksViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pinned = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    var loaded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var enabled by remember { mutableStateOf(true) }
    var rangePreset by remember { mutableStateOf(WebhookRangePreset.SINCE_LAST_SUCCESS) }
    var customStart by remember { mutableStateOf<LocalDateTime?>(null) }
    var customEnd by remember { mutableStateOf<LocalDateTime?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val selectedTypes = remember { mutableStateListOf(WebhookDataType.SUMMARY, WebhookDataType.TRANSACTIONS) }
    val headers = remember { mutableStateListOf<WebhookHeader>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profileId) {
        val draft = viewModel.loadDraft(profileId)
        name = draft.name
        url = draft.url
        currency = draft.currency
        enabled = draft.enabled
        rangePreset = draft.rangePreset
        customStart = draft.customStart
        customEnd = draft.customEnd
        selectedTypes.clear()
        selectedTypes.addAll(draft.dataTypes)
        headers.clear()
        headers.addAll(draft.headers.ifEmpty { listOf(WebhookHeader("", "")) })
        loaded = true
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = if (profileId == null) stringResource(R.string.new_webhook) else stringResource(R.string.edit_webhook),
                scrollBehaviorSmall = pinned,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        }
    ) { paddingValues ->
        if (!loaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box {
            Column(
                modifier = Modifier
                    .hazeSource(hazeState)
                    .fillMaxSize()
                    .overScrollVertical()
                    .imePadding()
                    .verticalScroll(state = rememberScrollState())
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SectionHeader(title = stringResource(R.string.webhook_endpoint_section))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    VittifyTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = WebhookValidation.validateName(it)
                        },
                        label = stringResource(R.string.webhook_name_label),
                        leading = Icons.Rounded.Badge,
                        shape = topFieldShape(),
                        errorMessage = nameError
                    )
                    VittifyTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            urlError = WebhookValidation.validateUrl(it)
                        },
                        label = stringResource(R.string.webhook_url_label),
                        leading = Icons.Rounded.Link,
                        shape = bottomFieldShape(),
                        errorMessage = urlError
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))
                SectionHeader(title = stringResource(R.string.webhook_data_types_section))
                Text(
                    text = stringResource(R.string.webhook_data_types_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    WebhookDataType.entries.forEach { type ->
                        val selected = selectedTypes.contains(type)
                        PillChip(
                            label = when (type) {
                                WebhookDataType.SUMMARY -> stringResource(R.string.datatype_summary)
                                WebhookDataType.TRANSACTIONS -> stringResource(R.string.datatype_transactions)
                                WebhookDataType.BUDGETS -> stringResource(R.string.datatype_budgets)
                                WebhookDataType.ACCOUNTS -> stringResource(R.string.datatype_accounts)
                                WebhookDataType.SUBSCRIPTIONS -> stringResource(R.string.datatype_subscriptions)
                            },
                            selected = selected,
                            onClick = {
                                if (selected) selectedTypes.remove(type) else selectedTypes.add(type)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xs))
                SectionHeader(title = stringResource(R.string.webhook_time_range_section))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    WebhookRangePreset.entries.forEach { preset ->
                        PillChip(
                            label = when (preset) {
                                WebhookRangePreset.SINCE_LAST_SUCCESS -> stringResource(R.string.range_since_last_success)
                                WebhookRangePreset.TODAY -> stringResource(R.string.range_today)
                                WebhookRangePreset.CURRENT_WEEK -> stringResource(R.string.range_current_week)
                                WebhookRangePreset.CURRENT_MONTH -> stringResource(R.string.range_current_month)
                                WebhookRangePreset.PREVIOUS_MONTH -> stringResource(R.string.range_previous_month)
                                WebhookRangePreset.LAST_30_DAYS -> stringResource(R.string.range_last_30_days_lower)
                                WebhookRangePreset.CUSTOM -> stringResource(R.string.range_custom)
                            },
                            selected = rangePreset == preset,
                            onClick = { rangePreset = preset }
                        )
                    }
                }

                if (rangePreset == WebhookRangePreset.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        DateField(
                            label = stringResource(R.string.starts),
                            value = customStart,
                            icon = Icons.Rounded.CalendarMonth,
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        DateField(
                            label = stringResource(R.string.ends),
                            value = customEnd,
                            icon = Icons.Rounded.Event,
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        Icons.Rounded.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.webhook_optional_headers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { headers.add(WebhookHeader("", "")) },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add), style = MaterialTheme.typography.labelMedium)
                    }
                }

                headers.forEachIndexed { index, header ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimensions.Padding.content),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Text(
                                    text = stringResource(R.string.header_index_format, index + 1),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { headers.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Iconax.Bag,
                                        contentDescription = stringResource(R.string.remove_header),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(1.5.dp)
                            ) {
                                VittifyTextField(
                                    value = header.key,
                                    onValueChange = { headers[index] = header.copy(key = it) },
                                    label = stringResource(R.string.header_name_label),
                                    leading = Icons.Rounded.VpnKey,
                                    shape = topFieldShape()
                                )
                                VittifyTextField(
                                    value = header.value,
                                    onValueChange = { headers[index] = header.copy(value = it) },
                                    label = stringResource(R.string.header_value_label),
                                    leading = Icons.Rounded.Code,
                                    shape = bottomFieldShape()
                                )
                            }
                        }
                    }
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(150.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Save button
                         Button(
                             onClick = {
                                 val draft = WebhookProfileDraft(
                                     id = profileId,
                                     name = name,
                                     url = url,
                                     enabled = enabled,
                                     dataTypes = selectedTypes.toSet(),
                                     rangePreset = rangePreset,
                                     customStart = customStart,
                                     customEnd = customEnd,
                                     currency = currency,
                                     headers = headers.filter { it.key.isNotBlank() }
                                 )
                                 viewModel.saveProfile(
                                     draft = draft,
                                     onSaved = { onNavigateBack() },
                                     onError = { errorMessage = it }
                                 )
                             },
                             modifier = Modifier
                                 .weight(1f)
                                 .height(56.dp),
                             enabled = name.isNotBlank() && url.isNotBlank() &&
                                     nameError == null && urlError == null &&
                                     isCustomRangeValid(rangePreset, customStart, customEnd),
                             shapes = ButtonDefaults.shapes()
                         ) {
                             Text(
                                 text = stringResource(R.string.save_webhook),
                                 fontSize = 16.sp,
                                 fontWeight = FontWeight.Bold
                             )
                         }
                         // Delete button (only for existing budgets)
                         if (profileId != null) {
                             OutlinedButton(
                                 onClick = { showDeleteDialog = true },
                                 modifier = Modifier.height(56.dp),
                                 colors = ButtonDefaults.outlinedButtonColors(
                                     containerColor = MaterialTheme.colorScheme.surface,
                                     contentColor = MaterialTheme.colorScheme.error
                                 ),
                                 border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                     brush = Brush.linearGradient(
                                         colors = listOf(
                                             MaterialTheme.colorScheme.error,
                                             MaterialTheme.colorScheme.error
                                         )
                                     )
                                 )
                             ) {
                                 Icon(
                                     imageVector = Iconax.Bag,
                                     contentDescription = stringResource(R.string.delete_webhook)
                                 )
                             }
                         }
                }
            }

            if (showDeleteDialog && profileId != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.delete_webhook_confirm_title)) },
                    text = {
                        Text(
                            stringResource(R.string.delete_webhook_confirm_desc)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.deleteProfile(profileId)
                                onNavigateBack()
                            }
                        ) {
                            Text(
                                stringResource(R.string.delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))


            if (showStartPicker) {
                // M3 DatePicker treats selectedDateMillis as midnight-UTC for the picked calendar
                // date. Round-trip through UTC so users in negative offsets don't see the date
                // shift back one day on save.
                val initialMillis = (customStart?.toLocalDate() ?: LocalDate.now())
                    .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                DatePicker(
                    onDismiss = { showStartPicker = false },
                    onConfirm = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC).toLocalDate()
                            customStart = pickedDate.atStartOfDay()
                        }
                        showStartPicker = false
                    },
                    datePickerState = pickerState
                )
            }

            if (showEndPicker) {
                val initialMillis = (customEnd?.toLocalDate() ?: LocalDate.now())
                    .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                DatePicker(
                    onDismiss = { showEndPicker = false },
                    onConfirm = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC).toLocalDate()
                            // End-of-day so the inclusive bound covers the picked day in full.
                            customEnd = pickedDate.atTime(LocalTime.MAX)
                        }
                        showEndPicker = false
                    },
                    datePickerState = pickerState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VittifyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leading: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    shape: Shape = singleFieldShape(),
    singleLine: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    suffix: (@Composable () -> Unit)? = null,
    errorMessage: String? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leading?.let {
            { Icon(it, contentDescription = null) }
        },
        trailingIcon = trailing,
        suffix = suffix,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    )
}

@Composable
internal fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .clip(VittifyShapes.pill)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor
        )
    }
}

@Composable
internal fun topFieldShape(): Shape = VittifyShapes.scaled(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 4.dp,
    bottomEnd = 4.dp
)

@Composable
internal fun middleFieldShape(): Shape = VittifyShapes.scaled(4.dp)

@Composable
internal fun bottomFieldShape(): Shape = VittifyShapes.scaled(
    topStart = 4.dp,
    topEnd = 4.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp
)

@Composable
internal fun singleFieldShape(): Shape = VittifyShapes.input

@Composable
private fun DateField(
    label: String,
    value: LocalDateTime?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = VittifyShapes.input,
        color = VittifySurface.surfaceContainerLowColor(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value?.format(DATE_FIELD_FORMATTER) ?: stringResource(R.string.pick_a_date),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val DATE_FIELD_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun isCustomRangeValid(
    preset: WebhookRangePreset,
    customStart: LocalDateTime?,
    customEnd: LocalDateTime?
): Boolean {
    if (preset != WebhookRangePreset.CUSTOM) return true
    if (customStart == null || customEnd == null) return false
    return !customStart.isAfter(customEnd)
}
