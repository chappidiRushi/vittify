package com.reddy.vittify.presentation.ui.features.globalsearch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.presentation.ui.components.BrandIcon
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.components.VittifyCard
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.ReceiptSearch
import com.reddy.vittify.presentation.ui.icons.Search
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.credit_dark
import com.reddy.vittify.presentation.ui.theme.credit_light
import com.reddy.vittify.presentation.ui.theme.expense_dark
import com.reddy.vittify.presentation.ui.theme.expense_light
import com.reddy.vittify.presentation.ui.theme.income_dark
import com.reddy.vittify.presentation.ui.theme.income_light
import com.reddy.vittify.presentation.ui.theme.investment_dark
import com.reddy.vittify.presentation.ui.theme.investment_light
import com.reddy.vittify.presentation.ui.theme.transfer_dark
import com.reddy.vittify.presentation.ui.theme.transfer_light
import com.reddy.vittify.utils.CurrencyFormatter
import com.reddy.vittify.utils.formatAmount
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.time.format.DateTimeFormatter

@Composable
fun GlobalSearchResultsOverlay(
    uiState: GlobalSearchUiState,
    onAccountClick: (bankName: String, accountLast4: String) -> Unit,
    onSettingClick: (setting: GlobalSearchSettingItem) -> Unit,
    onTransactionClick: (transactionId: Long) -> Unit,
    onPageClick: (destination: Any) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    blurEffects: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val hazeModifier = if (blurEffects && hazeState != null) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeDefaults.style(
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                blurRadius = 24.dp
            )
        )
    } else {
        Modifier.background(MaterialTheme.colorScheme.surface)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(hazeModifier)
            .padding(contentPadding)
    ) {
        when {
            uiState.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }

            uiState.query.isBlank() -> {
                SearchInitialState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.lg)
                )
            }

            uiState.results.isEmpty -> {
                SearchEmptyState(
                    query = uiState.query,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.lg)
                )
            }

            else -> {
                SearchResultsList(
                    results = uiState.results,
                    onAccountClick = onAccountClick,
                    onSettingClick = onSettingClick,
                    onTransactionClick = onTransactionClick,
                    onPageClick = onPageClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: GlobalSearchGroupedResults,
    onAccountClick: (bankName: String, accountLast4: String) -> Unit,
    onSettingClick: (setting: GlobalSearchSettingItem) -> Unit,
    onTransactionClick: (transactionId: Long) -> Unit,
    onPageClick: (destination: Any) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = Dimensions.Padding.content),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        contentPadding = PaddingValues(bottom = 120.dp, top = Spacing.sm)
    ) {
        // 1. Accounts Section
        if (results.accounts.isNotEmpty()) {
            item(key = "header_accounts") {
                SectionHeader(
                    title = stringResource(R.string.global_search_accounts) + " (${results.accounts.size})",
                    modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs)
                )
            }
            itemsIndexed(
                items = results.accounts,
                key = { _, it -> "acc_${it.bankName}_${it.accountLast4}" }
            ) { index, account ->
                GlobalSearchAccountRow(
                    account = account,
                    position = ListItemPosition.from(index, results.accounts.size),
                    onClick = { onAccountClick(account.bankName, account.accountLast4) }
                )
            }
        }

        // 2. Transactions Section
        if (results.transactions.isNotEmpty()) {
            item(key = "header_transactions") {
                SectionHeader(
                    title = stringResource(R.string.global_search_transactions) + " (${results.transactions.size})",
                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs)
                )
            }
            itemsIndexed(
                items = results.transactions,
                key = { _, it -> "tx_${it.id}" }
            ) { index, transaction ->
                GlobalSearchTransactionRow(
                    transaction = transaction,
                    position = ListItemPosition.from(index, results.transactions.size),
                    onClick = { onTransactionClick(transaction.id) }
                )
            }
        }

        // 3. Settings Section
        if (results.settings.isNotEmpty()) {
            item(key = "header_settings") {
                SectionHeader(
                    title = stringResource(R.string.global_search_settings) + " (${results.settings.size})",
                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs)
                )
            }
            itemsIndexed(
                items = results.settings,
                key = { _, it -> "setting_${it.id}" }
            ) { index, setting ->
                GlobalSearchSettingRow(
                    setting = setting,
                    position = ListItemPosition.from(index, results.settings.size),
                    onClick = { onSettingClick(setting) }
                )
            }
        }

        // 4. Pages Section
        if (results.pages.isNotEmpty()) {
            item(key = "header_pages") {
                SectionHeader(
                    title = stringResource(R.string.global_search_pages) + " (${results.pages.size})",
                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs)
                )
            }
            itemsIndexed(
                items = results.pages,
                key = { _, it -> "page_${it.id}" }
            ) { index, page ->
                GlobalSearchPageRow(
                    page = page,
                    position = ListItemPosition.from(index, results.pages.size),
                    onClick = { onPageClick(page.destination) }
                )
            }
        }
    }
}

@Composable
private fun GlobalSearchAccountRow(
    account: AccountBalanceEntity,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        headline = {
            Text(
                text = account.bankName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supporting = {
            val accountDesc = if (account.isCreditCard) {
                "•••• ${account.accountLast4} • Credit Card"
            } else {
                "•••• ${account.accountLast4}"
            }
            Text(
                text = accountDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leading = {
            BrandIcon(
                merchantName = account.bankName,
                accountIconResId = account.iconResId,
                accountIconName = account.iconName,
                accountColorHex = account.color,
                size = 40.dp
            )
        },
        trailing = {
            Text(
                text = CurrencyFormatter.formatCurrency(account.balance, account.currency),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        shape = position.toShape(),
        onClick = onClick,
        padding = PaddingValues(0.dp)
    )
}

@Composable
private fun GlobalSearchTransactionRow(
    transaction: TransactionEntity,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val amountColor = when (transaction.transactionType) {
        TransactionType.INCOME -> if (!isDark) income_light else income_dark
        TransactionType.EXPENSE -> if (!isDark) expense_light else expense_dark
        TransactionType.CREDIT -> if (!isDark) credit_light else credit_dark
        TransactionType.TRANSFER, TransactionType.BALANCE_UPDATE -> if (!isDark) transfer_light else transfer_dark
        TransactionType.INVESTMENT -> if (!isDark) investment_light else investment_dark
    }
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val dateStr = transaction.dateTime.format(dateFormatter)

    ListItem(
        modifier = modifier,
        headline = {
            Text(
                text = transaction.merchantName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supporting = {
            val categoryPart = transaction.category.ifEmpty { transaction.transactionType.name }
            Text(
                text = "$categoryPart • $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leading = {
            BrandIcon(
                merchantName = transaction.merchantName,
                category = transaction.category,
                subcategory = transaction.subcategory,
                size = 40.dp
            )
        },
        trailing = {
            Text(
                text = transaction.formatAmount(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        },
        shape = position.toShape(),
        onClick = onClick,
        padding = PaddingValues(0.dp)
    )
}

@Composable
private fun GlobalSearchSettingRow(
    setting: GlobalSearchSettingItem,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        headline = {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supporting = {
            val subtitleText = if (!setting.parentSection.isNullOrBlank()) {
                "${setting.parentSection} • ${setting.subtitle}"
            } else {
                setting.subtitle
            }
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leading = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(setting.iconBg.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = setting.iconVector,
                    contentDescription = null,
                    tint = setting.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailing = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = position.toShape(),
        onClick = onClick,
        padding = PaddingValues(0.dp)
    )
}

@Composable
private fun GlobalSearchPageRow(
    page: GlobalSearchPageItem,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        headline = {
            Text(
                text = page.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supporting = {
            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leading = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.iconVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailing = {
            SuggestionChip(
                onClick = onClick,
                label = {
                    Text(
                        text = stringResource(R.string.global_search_page_badge),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = null,
                shape = VittifyShapes.small
            )
        },
        shape = position.toShape(),
        onClick = onClick,
        padding = PaddingValues(0.dp)
    )
}

@Composable
private fun SearchInitialState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        VittifyCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Iconax.Search,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = stringResource(R.string.global_search_hint_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.global_search_hint_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        VittifyCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Iconax.ReceiptSearch,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = stringResource(R.string.global_search_no_results, query),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.global_search_no_results_tip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

