package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.generator.HistoryDuration
import com.reddy.vittify.data.generator.RandomDataConfig
import com.reddy.vittify.data.generator.RandomDataPreset
import com.reddy.vittify.data.generator.TransactionDensity
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.utils.CurrencyFormatter

private val SUPPORTED_CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "JPY")
private val ACCOUNT_COUNT_OPTIONS = listOf(2, 3, 4, 5, 6, 8)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RandomDataConfigSheet(
    initialConfig: RandomDataConfig = RandomDataConfig(),
    onDismiss: () -> Unit,
    onGenerate: (RandomDataConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var config by remember { mutableStateOf(initialConfig) }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    VittifyModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = VittifyShapes.bottomSheet,
        containerColor = VittifySurface.surfaceContainerLowColor(),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = Spacing.sm, bottom = Spacing.xs)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(VittifyShapes.pill)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
        ) {
            // Sticky Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column {
                    Text(
                        text = "Generate Random Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize accounts, timeline, currencies & features",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Scrollable configuration body
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 1. Predefined Quick Presets
                SectionHeader(
                    title = "Predefined Presets",
                    trailingAction = {
                        Surface(
                            shape = VittifyShapes.pill,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .clip(VittifyShapes.pill)
                                .clickable {
                                    config = RandomDataPreset.SURPRISE_ME.toConfig()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Randomize All",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    RandomDataPreset.entries.forEach { preset ->
                        PresetSelectionCard(
                            preset = preset,
                            isSelected = config.preset == preset,
                            onClick = {
                                config = preset.toConfig()
                            }
                        )
                    }
                }

                // 2. Date Range / Timeline Duration
                Surface(
                    shape = VittifyShapes.platter,
                    color = VittifySurface.surfaceContainerColor(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        SectionHeader(title = "Date Range (Timeline History)")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            HistoryDuration.entries.forEach { duration ->
                                val selected = config.historyDuration == duration
                                FilterChip(
                                    selected = selected,
                                    onClick = { config = config.copy(historyDuration = duration) },
                                    label = {
                                        Text(
                                            text = duration.label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    shape = VittifyShapes.pill
                                )
                            }
                        }

                        // 3. Number of Accounts
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        SectionHeader(title = "Number of Bank & Wallet Accounts")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            ACCOUNT_COUNT_OPTIONS.forEach { count ->
                                val selected = config.accountCount == count
                                val bg by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.primary else VittifySurface.surfaceContainerHighColor(),
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "accCountBg_$count"
                                )
                                val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(VittifyShapes.input)
                                        .clickable { config = config.copy(accountCount = count) },
                                    color = bg,
                                    shape = VittifyShapes.input
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = "Includes Savings, Credit Cards, Current & Cash Wallets with distinct 4-digit suffixes.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Currency Configuration
                Surface(
                    shape = VittifyShapes.platter,
                    color = VittifySurface.surfaceContainerColor(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(title = "Currencies & Foreign Exchange")
                            FilterChip(
                                selected = config.isMultiCurrency,
                                onClick = {
                                    val nextMulti = !config.isMultiCurrency
                                    val defaultSecondaries = if (nextMulti && config.secondaryCurrencies.isEmpty()) {
                                        (setOf("USD", "EUR", "AED") - config.baseCurrency)
                                    } else config.secondaryCurrencies
                                    config = config.copy(
                                        isMultiCurrency = nextMulti,
                                        secondaryCurrencies = defaultSecondaries
                                    )
                                },
                                label = {
                                    Text(
                                        text = if (config.isMultiCurrency) "Multi-Currency ON" else "Single Currency",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.CurrencyExchange,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                shape = VittifyShapes.pill
                            )
                        }

                        Text(
                            text = "Primary Base Currency",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val allBaseCurrencies = remember(config.baseCurrency) {
                            if (config.baseCurrency !in SUPPORTED_CURRENCIES) {
                                SUPPORTED_CURRENCIES + config.baseCurrency
                            } else {
                                SUPPORTED_CURRENCIES
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            allBaseCurrencies.forEach { code ->
                                val selected = config.baseCurrency == code
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        config = config.copy(
                                            baseCurrency = code,
                                            secondaryCurrencies = config.secondaryCurrencies - code
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "${CurrencyFormatter.getCurrencySymbol(code)} $code",
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    shape = VittifyShapes.pill
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = { showCurrencyPicker = true },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Other",
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                shape = VittifyShapes.pill
                            )
                        }

                        AnimatedVisibility(visible = config.isMultiCurrency) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Text(
                                    text = "Foreign Account Currencies (Select 1 or more)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    SUPPORTED_CURRENCIES.filter { it != config.baseCurrency }.forEach { code ->
                                        val selected = code in config.secondaryCurrencies
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                val updated = if (selected && config.secondaryCurrencies.size > 1) {
                                                    config.secondaryCurrencies - code
                                                } else {
                                                    config.secondaryCurrencies + code
                                                }
                                                config = config.copy(secondaryCurrencies = updated)
                                            },
                                            label = {
                                                Text("${CurrencyFormatter.getCurrencySymbol(code)} $code")
                                            },
                                            leadingIcon = if (selected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            } else null,
                                            shape = VittifyShapes.pill
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Transaction Density & Feature Modules
                Surface(
                    shape = VittifyShapes.platter,
                    color = VittifySurface.surfaceContainerColor(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        SectionHeader(title = "Transaction Density")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            TransactionDensity.entries.forEach { density ->
                                val selected = config.density == density
                                FilterChip(
                                    selected = selected,
                                    onClick = { config = config.copy(density = density) },
                                    label = {
                                        Text(
                                            text = "${density.label} (~${density.avgTxPerMonth}/mo)",
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                        )
                                    },
                                    shape = VittifyShapes.pill
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.xs))
                        SectionHeader(title = "Included Vittify Features")
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FeatureToggleChip(
                                label = "Transfers & CC Bills",
                                checked = config.includeTransfersAndCcPayments,
                                onToggle = { config = config.copy(includeTransfersAndCcPayments = it) }
                            )
                            FeatureToggleChip(
                                label = "Budgets & Limits",
                                checked = config.includeBudgets,
                                onToggle = { config = config.copy(includeBudgets = it) }
                            )
                            FeatureToggleChip(
                                label = "Subscriptions",
                                checked = config.includeSubscriptions,
                                onToggle = { config = config.copy(includeSubscriptions = it) }
                            )
                            FeatureToggleChip(
                                label = "SIP Investments",
                                checked = config.includeInvestments,
                                onToggle = { config = config.copy(includeInvestments = it) }
                            )
                            FeatureToggleChip(
                                label = "Debit & Credit Cards",
                                checked = config.includeCards,
                                onToggle = { config = config.copy(includeCards = it) }
                            )
                        }
                    }
                }

                // 6. Balance & Volume Summary Card
                Surface(
                    shape = VittifyShapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Estimated Volume",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Est. ~${config.estimatedTransactionsCount} transactions across ${config.accountCount} accounts (${config.historyDuration.label}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Primary Generate Button
            Button(
                onClick = { onGenerate(config) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = VittifyShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Generate ${config.historyDuration.label} of Random Data",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }

        if (showCurrencyPicker) {
            CurrencyBottomSheet(
                selectedCurrency = config.baseCurrency,
                onCurrencySelected = { code ->
                    config = config.copy(
                        baseCurrency = code,
                        secondaryCurrencies = config.secondaryCurrencies - code
                    )
                    showCurrencyPicker = false
                },
                onDismiss = { showCurrencyPicker = false }
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        trailingAction?.invoke()
    }
}

@Composable
private fun PresetSelectionCard(
    preset: RandomDataPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            VittifySurface.surfaceContainerColor()
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "presetCardColor_${preset.name}"
    )

    val borderStroke = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(VittifyShapes.input)
            .clickable(onClick = onClick),
        shape = VittifyShapes.input,
        color = containerColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    preset.badge?.let { badge ->
                        Surface(
                            shape = VittifyShapes.pill,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = preset.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleChip(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    FilterChip(
        selected = checked,
        onClick = { onToggle(!checked) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium
            )
        },
        leadingIcon = if (checked) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = VittifyShapes.pill
    )
}
