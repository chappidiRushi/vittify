package com.reddy.vittify.presentation.ui.features.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.navigation.TransactionDetail
import com.reddy.vittify.presentation.navigation.safeNavigate
import com.reddy.vittify.presentation.navigation.safePopBackStack
import com.reddy.vittify.presentation.ui.components.AccountCard
import com.reddy.vittify.presentation.ui.components.BalanceChart
import com.reddy.vittify.presentation.ui.components.BalancePoint
import com.reddy.vittify.presentation.ui.components.VittifyCard
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.LoadingCircle
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.components.TransactionItem
import com.reddy.vittify.presentation.ui.components.TransactionTotalsCard
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.ReceiptItem
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AccountDetailScreen(
    navController: NavController,
    bankName: String = "",
    accountLast4: String = "",
    accountDetailViewModel: AccountDetailViewModel = hiltViewModel(),
    animatedContentScope: AnimatedVisibilityScope? = null
) {
    val uiState by accountDetailViewModel.uiState.collectAsState()
    val selectedDateRange by accountDetailViewModel.selectedDateRange.collectAsState()
    val categoriesMap by accountDetailViewModel.categoriesMap.collectAsStateWithLifecycle()
    val subcategoriesMap by accountDetailViewModel.subcategoriesMap.collectAsStateWithLifecycle()
    
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (animatedContentScope != null) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "account_${bankName}_${accountLast4}"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = { _, _ ->
                            spring(
                                stiffness =  Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            )
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Fit, Alignment.Center)
                    )
                        .skipToLookaheadSize()
                } else Modifier
            )
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = uiState.bankName.ifEmpty { stringResource(R.string.account_details) },
                hasBackButton = true,
                hazeState = hazeState,
                navigationContent = { NavigationContent { navController.safePopBackStack() } },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .hazeSource(state = hazeState),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
            contentPadding = PaddingValues(
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding()
            ),
        ) {
            // Account Card
            item {
                uiState.currentBalance?.let { balance ->
                    AccountCard(
                        account = balance,
                        showMoreOptions = false,
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                    )
                }
            }
            item{
                Spacer(Modifier.height(Spacing.md))
            }

            // Date Range Filter
            item {
                DateRangeFilter(
                    selectedRange = selectedDateRange,
                    onRangeSelected = accountDetailViewModel::selectDateRange
                )
            }
            item{
                Spacer(Modifier.height(Spacing.md))
            }

            // Balance Chart
            if (uiState.balanceChartData.isNotEmpty()) {
                item {
                    ExpandableBalanceChart(
                        primaryCurrency = uiState.primaryCurrency,
                        balanceHistory = uiState.balanceChartData,
                        selectedTimeframe = selectedDateRange.getLocalizedLabel(),
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                    )
                }
            }
            item{
                Spacer(Modifier.height(Spacing.md))
            }
            // Summary Statistics
            item {
                TransactionTotalsCard(
                    income = uiState.totalIncome,
                    expenses = uiState.totalExpenses,
                    netBalance = uiState.netBalance,
                    currency = uiState.primaryCurrency,
                    title = selectedDateRange.getLocalizedLabel(),
                    isEstimated = uiState.hasMultipleCurrencies,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                )
            }
            item{
                Spacer(Modifier.height(Spacing.md))
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.transactions_count, uiState.transactions.size),
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content + Spacing.sm)
                )
            }

            item{
                Spacer(Modifier.height(Spacing.sm))
            }
            // Transaction List
            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyTransactionsState(
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.transactions,
                    key = { _, it -> it.id }
                ) { index, transaction ->
                    val categoryEntity = categoriesMap[transaction.category]
                    val subcategoryEntity = if (categoryEntity != null && transaction.subcategory != null) {
                        subcategoriesMap[transaction.subcategory]
                    } else null

                    val position = remember(index, uiState.transactions.size) {
                        ListItemPosition.from(index, uiState.transactions.size)
                    }
                    val shape = position.toShape()

                    this@AccountDetailScreen.TransactionItem(
                        transaction = transaction,
                        categoryEntity = categoryEntity,
                        subcategoryEntity = subcategoryEntity,
                        balanceAfter = transaction.balanceAfter,
                        balanceCurrency = uiState.primaryCurrency,
                        accountIconResId = uiState.currentBalance?.iconResId ?: 0,
                        accountIconName = uiState.currentBalance?.iconName,
                        accountColorHex = uiState.currentBalance?.color,
                        convertedAmount = uiState.convertedAmounts[transaction.id],
                        mainCurrency = uiState.baseCurrency,
                        currentAccountContext = uiState.currentBalance?.accountLast4,
                        currentBankNameContext = bankName,
                        isSplit = uiState.splitTransactionIds.contains(transaction.id),
                        onClick = {
                            navController.safeNavigate(
                                TransactionDetail(
                                    transactionId = transaction.id,
                                    sharedElementKey = "transaction_${transaction.id}"
                                )
                            )
                        },
                        shape = shape,
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                        animatedContentScope = animatedContentScope,
                        sharedElementKey = "transaction_${transaction.id}"
                    )
                }
            }
            item{
                Spacer(Modifier.height(Spacing.md))
            }
            // Loading State
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingCircle()
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ExpandableBalanceChart(
    modifier: Modifier = Modifier,
    primaryCurrency: String,
    balanceHistory: List<BalancePoint>,
    selectedTimeframe: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    VittifyCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ){ isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.balance_trend),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = selectedTimeframe,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
                
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (isExpanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    BalanceChart(
                        primaryCurrency = primaryCurrency,
                        balanceHistory = balanceHistory,
                        height = 180
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilter(
    selectedRange: DateRange,
    onRangeSelected: (DateRange) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        item{
            Spacer(modifier = Modifier.width(Spacing.md))
        }
        items(DateRange.values().toList()) { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.getLocalizedLabel()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderWidth = 0.dp,
                    selected = selectedRange == range,
                    enabled = true
                ),
            )
        }
        item{
            Spacer(modifier = Modifier.width(Spacing.md))
        }
    }
}


@Composable
private fun EmptyTransactionsState(
    modifier: Modifier = Modifier
) {
    VittifyCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Iconax.ReceiptItem,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.no_transactions_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.transactions_appear_here),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DateRange.getLocalizedLabel(): String {
    return when (this) {
        DateRange.LAST_7_DAYS -> stringResource(R.string.range_last_7_days)
        DateRange.LAST_30_DAYS -> stringResource(R.string.range_last_30_days)
        DateRange.LAST_3_MONTHS -> stringResource(R.string.range_last_3_months)
        DateRange.LAST_6_MONTHS -> stringResource(R.string.range_last_6_months)
        DateRange.LAST_YEAR -> stringResource(R.string.range_last_year)
        DateRange.ALL_TIME -> stringResource(R.string.range_all_time)
    }
}