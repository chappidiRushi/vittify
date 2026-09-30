package com.reddy.vittify.presentation.ui.features.onboarding.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CreditScore
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.components.CurrencyBottomSheet
import com.reddy.vittify.presentation.ui.features.accounts.AccountType
import com.reddy.vittify.presentation.ui.icons.Edit2
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.utils.CurrencyFormatter

/**
 * Step 4: Primary Account Creation.
 * Full-featured account setup mirroring AddAccountScreen and EditAccountSheet with real-time live preview,
 * type selector, balance, currency picker, last 4 digits, credit limits, and color swatches.
 */
@Composable
fun AccountCreationStep(
    accountType: AccountType,
    accountName: String,
    balance: String,
    currency: String,
    last4: String,
    creditLimit: String,
    selectedColorHex: String,
    selectedIconResId: Int,
    errorMessage: String?,
    onTypeChange: (AccountType) -> Unit,
    onNameChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onLast4Change: (String) -> Unit,
    onCreditLimitChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSaveAndContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCurrencySheet by remember { mutableStateOf(false) }
    val last4BringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    val presetColors = remember {
        listOf(
            "#33B5E5", // Ocean Blue
            "#00C853", // Emerald Green
            "#7C4DFF", // Royal Purple
            "#FF5252", // Soft Coral
            "#00B4D8", // Cyan
            "#FFA000", // Warm Amber
            "#E91E63", // Rose Pink
            "#263238"  // Slate Dark
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
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
                text = stringResource(R.string.create_account_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.create_account_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Live Account Preview Card
        LiveAccountPreviewCard(
            accountName = accountName,
            accountType = accountType,
            balance = balance,
            currency = currency,
            last4 = last4,
            colorHex = selectedColorHex,
            iconResId = selectedIconResId
        )

        // Error Banner
        AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = VittifyShapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // 2. Account Type Selector Tabs
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.surfaceContainerLowColor()
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.account_type_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AccountType.values()) { type ->
                        val isSelected = accountType == type
                        Surface(
                            onClick = { onTypeChange(type) },
                            shape = VittifyShapes.pill,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else VittifySurface.surfaceContainerHighColor(),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = when (type) {
                                    AccountType.SAVINGS -> stringResource(R.string.type_savings)
                                    AccountType.CURRENT -> stringResource(R.string.type_current)
                                    AccountType.CREDIT -> stringResource(R.string.type_credit)
                                    AccountType.WALLET -> stringResource(R.string.type_wallet)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Account Name Input
        TextField(
            value = accountName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.account_name_field_label)) },
            placeholder = { Text(stringResource(R.string.account_name_field_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.input,
            leadingIcon = { Icon(Iconax.Edit2, contentDescription = null, modifier = Modifier.size(20.dp)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                unfocusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        // 4. Starting Balance & Currency Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            TextField(
                value = balance,
                onValueChange = onBalanceChange,
                label = { Text(stringResource(R.string.initial_balance_field_label)) },
                modifier = Modifier.weight(1f),
                shape = VittifyShapes.input,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = {
                    Text(
                        text = CurrencyFormatter.getCurrencySymbol(currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                    unfocusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Currency Selector Button
            Card(
                onClick = { showCurrencySheet = true },
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 84.dp),
                shape = VittifyShapes.input,
                colors = CardDefaults.cardColors(
                    containerColor = VittifySurface.surfaceContainerLowColor()
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = Spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 5. Last 4 Digits (for bank & credit cards)
        if (accountType != AccountType.WALLET) {
            TextField(
                value = last4,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) onLast4Change(it) },
                label = { Text(stringResource(R.string.last_4_field_label)) },
                placeholder = { Text(stringResource(R.string.last_4_field_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(last4BringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                last4BringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                shape = VittifyShapes.input,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Rounded.Pin, contentDescription = null, modifier = Modifier.size(20.dp)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                    unfocusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // 6. Credit Limit (only for Credit Card)
        if (accountType == AccountType.CREDIT) {
            TextField(
                value = creditLimit,
                onValueChange = onCreditLimitChange,
                label = { Text(stringResource(R.string.credit_limit_field_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = VittifyShapes.input,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Rounded.CreditScore, contentDescription = null, modifier = Modifier.size(20.dp)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                    unfocusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // 7. Card Accent Color Swatches
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.surfaceContainerLowColor()
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.account_color_palette),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetColors) { hex ->
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(hex))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorChange(hex) }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // Save & Continue Action
        val isFormValid = accountName.isNotBlank() && balance.isNotBlank() &&
                (accountType == AccountType.WALLET || last4.length == 4)

        TactilePrimaryButton(
            text = stringResource(R.string.save_and_continue_action),
            onClick = onSaveAndContinue,
            enabled = isFormValid
        )

        Spacer(modifier = Modifier.height(Spacing.xs))
    }

    if (showCurrencySheet) {
        CurrencyBottomSheet(
            selectedCurrency = currency,
            onCurrencySelected = {
                onCurrencyChange(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }
}

