package com.reddy.vittify.presentation.ui.features.accounts

import com.reddy.vittify.utils.sumOfBigDecimal
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.CardType
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.AccountCard
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.DeleteAccountDialog
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.icons.Bag
import com.reddy.vittify.presentation.ui.icons.EyeSlash
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.LocalBlurEffects
import com.reddy.vittify.presentation.ui.theme.LocalVittifyTokens
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.utils.CurrencyFormatter
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeApi::class
)
@Composable
fun ManageAccountsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccountDetail: (String, String) -> Unit,
    onNavigateToAccountAudit: (String, String) -> Unit,
    onNavigateToDataSanitization: (String?) -> Unit = {},
    manageAccountsViewModel: ManageAccountsViewModel = hiltViewModel(),
    blurEffects: Boolean,
) {
    val uiState by manageAccountsViewModel.uiState.collectAsState()
    val defaultCurrency by manageAccountsViewModel.defaultCurrencyForNewAccounts.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedAccountEntity by remember {
        mutableStateOf<AccountBalanceEntity?>(null)
    }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<AccountBalanceEntity?>(null) }
    var showHiddenAccounts by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var accountToEdit by remember {
        mutableStateOf<AccountBalanceEntity?>(null)
    }
    var showSettleSheet by remember { mutableStateOf(false) }
    var cardToSettle by remember { mutableStateOf<AccountBalanceEntity?>(null) }

    var showDiagnosticsSheet by remember { mutableStateOf(false) }
    var showFloatingLabel by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Merge Flow States
    var showMergeSelection by remember { mutableStateOf(false) }
    var showMergeBalanceOption by remember { mutableStateOf(false) }
    var showMergeConfirmation by remember { mutableStateOf(false) }
    var showMergeManualInput by remember { mutableStateOf(false) }
    var accountForMerge by remember { mutableStateOf<AccountBalanceEntity?>(null) }

    var selectedMergeAccounts by remember {
        mutableStateOf<List<AccountBalanceEntity>>(emptyList())
    }
    var mergeNewBalance by remember { mutableStateOf<BigDecimal?>(null) }
    var selectedCardForLink by remember { mutableStateOf<CardEntity?>(null) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstVisibleItem ->
            // Show the label only when the list is scrolled to the top
            showFloatingLabel = firstVisibleItem == 0
        }
    }

    // Show snackbar messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                manageAccountsViewModel.clearError()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.title_accounts),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) },
                actionContent = {
                    IconButton(onClick = { onNavigateToDataSanitization("accounts") }) {
                        BadgedBox(
                            badge = {
                                if (uiState.issueCount > 0) {
                                    Badge { Text(uiState.issueCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HealthAndSafety,
                                contentDescription = "Account Health & Diagnostics"
                            )
                        }
                    }
                }
            ) },
        floatingActionButton = {
            val fabContainerColor = MaterialTheme.colorScheme.primaryContainer
            val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                expanded = showFloatingLabel,
                icon = { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_account_fab_desc)) },
                text = { Text(text = stringResource(R.string.add_account_fab_desc)) },
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = (LocalBottomNavPadding.current - 16.dp).coerceAtLeast(0.dp))
                    .height(48.dp),
                containerColor = fabContainerColor,
                contentColor = fabContentColor
            ) },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = LocalBottomNavPadding.current),
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor =
                            MaterialTheme.colorScheme
                                .onSecondaryContainer,
                        containerColor =
                            MaterialTheme.colorScheme
                                .secondaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.accounts.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.no_accounts_yet),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.add_account_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = hazeState)
                        .overScrollVertical(),
                    flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                    contentPadding = PaddingValues(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content +
                                paddingValues.calculateTopPadding(),
                        bottom = Dimensions.Padding.content +
                                paddingValues.calculateBottomPadding() + LocalBottomNavPadding.current
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Separate visible and hidden accounts
                    val visibleRegularAccounts = uiState.accounts.filter {
                        !it.isCreditCard && !it.isWallet && !manageAccountsViewModel.isAccountHidden(
                            it.bankName,
                            it.accountLast4
                        )
                    }
                    val visibleCreditCards = uiState.accounts.filter {
                        it.isCreditCard && !manageAccountsViewModel.isAccountHidden(
                            it.bankName,
                            it.accountLast4
                        )
                    }
                    val hiddenRegularAccounts = uiState.accounts.filter {
                        !it.isCreditCard && !it.isWallet && manageAccountsViewModel.isAccountHidden(
                            it.bankName,
                            it.accountLast4
                        )
                    }
                    val hiddenCreditCards = uiState.accounts.filter {
                        it.isCreditCard && manageAccountsViewModel.isAccountHidden(
                            it.bankName,
                            it.accountLast4
                        )
                    }
                    val allRegularAccounts = uiState.accounts.filter { !it.isCreditCard && !it.isWallet }
                    val wallets = uiState.accounts.filter { it.isWallet }


                    // Account Health Diagnostic Banner
                    item {
                        val tokens = LocalVittifyTokens.current
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDataSanitization("accounts") },
                            shape = VittifyShapes.platter,
                            border = VittifySurface.platterBorder(
                                strokeColor = if (uiState.issueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.issueCount > 0)
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f * tokens.surfaceOpacity)
                                else VittifySurface.platterContainerColor()
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.issueCount > 0) Icons.Rounded.HealthAndSafety else Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (uiState.issueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "Account Health & Diagnostics",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (uiState.issueCount > 0)
                                                "${uiState.issueCount} issue(s) detected (Hidden, Duplicate, or Bad Accounts)"
                                            else "Check for hidden, duplicate, or bad accounts",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Wallets Section
                    if (wallets.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.section_wallets),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                wallets.forEachIndexed { index, account ->
                                    AccountItem(
                                        account = account,
                                        linkedCards = emptyList(),
                                        position = ListItemPosition.Single,
                                        isHidden = false,
                                        isMain = uiState.mainAccountKey == "${account.bankName}_${account.accountLast4}",
                                        onSetAsMain = {
                                            manageAccountsViewModel.setAsMainAccount(
                                                account.bankName,
                                                account.accountLast4
                                            )
                                        },
                                        onToggleVisibility = {
                                            manageAccountsViewModel.toggleAccountVisibility(
                                                account.bankName,
                                                account.accountLast4
                                            )
                                        },
                                        onUpdateBalance = {
                                            selectedAccount = account.bankName to account.accountLast4
                                            selectedAccountEntity = account
                                            showUpdateDialog = true
                                        },
                                        onViewHistory = {
                                            historyAccount = account.bankName to account.accountLast4
                                            manageAccountsViewModel.loadBalanceHistory(
                                                account.bankName,
                                                account.accountLast4
                                            )
                                            showHistoryDialog = true
                                        },
                                        onAuditBalance = {
                                            onNavigateToAccountAudit(account.bankName, account.accountLast4)
                                        },
                                        onUnlinkCard = {},
                                        onDeleteAccount = {
                                            accountToDelete = account
                                            showDeleteConfirmDialog = true
                                        },
                                        onEditAccount = {
                                            accountToEdit = account
                                            showEditSheet = true
                                        },
                                        onAccountClick = {
                                            onNavigateToAccountDetail(account.bankName, account.accountLast4)
                                        },
                                        onMergeAccount = {
                                            accountForMerge = account
                                            showMergeSelection = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Regular Bank Accounts Section (Visible Only)
                    if (visibleRegularAccounts.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.section_bank_accounts),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                visibleRegularAccounts.forEachIndexed { index, account ->
                                    AccountItem(
                                        account = account,
                                        linkedCards = uiState.linkedCards[account.accountLast4]
                                            ?: emptyList(),
                                        position = ListItemPosition.Single,
                                        isHidden = false,
                                        isMain = uiState.mainAccountKey == "${account.bankName}_${account.accountLast4}",
                                        onSetAsMain = {
                                            manageAccountsViewModel.setAsMainAccount(
                                                account.bankName,
                                                account.accountLast4
                                            )
                                        },
                                        onToggleVisibility = {
                                            manageAccountsViewModel.toggleAccountVisibility(
                                                account.bankName,
                                                account.accountLast4
                                            )
                                        },
                                        onUpdateBalance = {
                                            selectedAccount = account.bankName to account.accountLast4
                                            selectedAccountEntity = account
                                            showUpdateDialog = true
                                        },
                                        onViewHistory = {
                                            historyAccount = account.bankName to account.accountLast4
                                            manageAccountsViewModel.loadBalanceHistory(
                                                account.bankName,
                                                account.accountLast4
                                            )
                                            showHistoryDialog = true
                                        },
                                        onAuditBalance = {
                                            onNavigateToAccountAudit(account.bankName, account.accountLast4)
                                        },
                                        onUnlinkCard = { cardId ->
                                            manageAccountsViewModel.unlinkCard(cardId)
                                        },
                                        onDeleteAccount = {
                                            accountToDelete = account
                                            showDeleteConfirmDialog = true
                                        },
                                        onEditAccount = {
                                            accountToEdit = account
                                            showEditSheet = true
                                        },
                                        onAccountClick = {
                                            onNavigateToAccountDetail(account.bankName, account.accountLast4)
                                        },
                                        onMergeAccount = {
                                            accountForMerge = account
                                            showMergeSelection = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // Orphaned Cards Section
                    if (uiState.orphanedCards.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(Spacing.md))

                            SectionHeader(
                                title = stringResource(R.string.section_unlinked_cards),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        items(uiState.orphanedCards) { card ->
                            OrphanedCardItem(
                                card = card,
                                accounts = allRegularAccounts,
                                onLinkToAccount = {
                                    selectedCardForLink = card
                                },
                                onDeleteCard = { cardId ->
                                    manageAccountsViewModel.deleteCard(cardId)
                                }
                            )
                        }
                    }
                    // Credit Cards Section (Visible Only)
                    if (visibleCreditCards.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            SectionHeader(
                                title = stringResource(R.string.section_credit_cards),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                visibleCreditCards.forEachIndexed { index, card ->
                                    CreditCardItem(
                                        card = card,
                                        position = ListItemPosition.Single,
                                        isHidden = false,
                                        isMain = uiState.mainAccountKey == "${card.bankName}_${card.accountLast4}",
                                        onSetAsMain = {
                                            manageAccountsViewModel.setAsMainAccount(
                                                card.bankName,
                                                card.accountLast4
                                            )
                                        },
                                        onToggleVisibility = {
                                            manageAccountsViewModel.toggleAccountVisibility(
                                                card.bankName,
                                                card.accountLast4
                                            )
                                        },
                                        onUpdateBalance = {
                                            selectedAccount = card.bankName to card.accountLast4
                                            selectedAccountEntity = card
                                            showUpdateDialog = true
                                        },
                                        onViewHistory = {
                                            historyAccount = card.bankName to card.accountLast4
                                            manageAccountsViewModel.loadBalanceHistory(
                                                card.bankName,
                                                card.accountLast4
                                            )
                                            showHistoryDialog = true
                                        },
                                        onAuditBalance = {
                                            onNavigateToAccountAudit(card.bankName, card.accountLast4)
                                        },
                                        onDeleteAccount = {
                                            accountToDelete = card
                                            showDeleteConfirmDialog = true
                                        },
                                        onEditAccount = {
                                            accountToEdit = card
                                            showEditSheet = true
                                        },
                                        onAccountClick = {
                                            onNavigateToAccountDetail(card.bankName, card.accountLast4)
                                        },
                                        onMergeAccount = {
                                            accountForMerge = card
                                            showMergeSelection = true
                                        },
                                        onSettleBill = {
                                            cardToSettle = card
                                            showSettleSheet = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Hidden Accounts Section
                    if (hiddenRegularAccounts.isNotEmpty() || hiddenCreditCards.isNotEmpty()
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        onClick = { showHiddenAccounts = !showHiddenAccounts },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ),
                                shape = VittifyShapes.platter,
                                border = VittifySurface.platterBorder(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f * LocalVittifyTokens.current.surfaceOpacity)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimensions.Padding.content),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        Icon(
                                            Iconax.EyeSlash,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = stringResource(R.string.hidden_accounts_count, hiddenRegularAccounts.size + hiddenCreditCards.size),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        if (showHiddenAccounts)
                                            Icons.Rounded.ExpandLess
                                        else
                                            Icons.Rounded.ExpandMore,
                                        contentDescription = if (showHiddenAccounts) stringResource(R.string.collapse) else stringResource(R.string.expand),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (showHiddenAccounts) {
                            if (hiddenRegularAccounts.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        hiddenRegularAccounts.forEachIndexed { index, account ->
                                            AccountItem(
                                                account = account,
                                                linkedCards = uiState.linkedCards[account.accountLast4] ?: emptyList(),
                                                position = ListItemPosition.Single,
                                                isHidden = true,
                                                isMain = uiState.mainAccountKey == "${account.bankName}_${account.accountLast4}",
                                                onSetAsMain = {
                                                    manageAccountsViewModel.setAsMainAccount(
                                                        account.bankName,
                                                        account.accountLast4
                                                    )
                                                },
                                                onToggleVisibility = {
                                                    manageAccountsViewModel.toggleAccountVisibility(
                                                        account.bankName,
                                                        account.accountLast4
                                                    )
                                                },
                                                onUpdateBalance = {
                                                    selectedAccount =
                                                        account.bankName to account.accountLast4
                                                    selectedAccountEntity = account
                                                    showUpdateDialog = true
                                                },
                                                onViewHistory = {
                                                    historyAccount =
                                                        account.bankName to account.accountLast4
                                                    manageAccountsViewModel.loadBalanceHistory(
                                                        account.bankName,
                                                        account.accountLast4
                                                    )
                                                    showHistoryDialog = true
                                                },
                                                onAuditBalance = {
                                                    onNavigateToAccountAudit(account.bankName, account.accountLast4)
                                                },
                                                onUnlinkCard = { cardId ->
                                                    manageAccountsViewModel.unlinkCard(cardId)
                                                },
                                                onDeleteAccount = {
                                                    accountToDelete =
                                                        account
                                                    showDeleteConfirmDialog = true
                                                },
                                                onEditAccount = {
                                                    accountToEdit = account
                                                    showEditSheet = true
                                                },
                                                onAccountClick = {
                                                    onNavigateToAccountDetail(account.bankName, account.accountLast4)
                                                },
                                                onMergeAccount = {
                                                    accountForMerge = account
                                                    showMergeSelection = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (hiddenCreditCards.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        hiddenCreditCards.forEachIndexed { index, card ->
                                            CreditCardItem(
                                                card = card,
                                                position = ListItemPosition.Single,
                                                isHidden = true,
                                                isMain = uiState.mainAccountKey == "${card.bankName}_${card.accountLast4}",
                                                onSetAsMain = {
                                                    manageAccountsViewModel.setAsMainAccount(
                                                        card.bankName,
                                                        card.accountLast4
                                                    )
                                                },
                                                onToggleVisibility = {
                                                    manageAccountsViewModel.toggleAccountVisibility(
                                                        card.bankName,
                                                        card.accountLast4
                                                    )
                                                },
                                                onUpdateBalance = {
                                                    selectedAccount = card.bankName to card.accountLast4
                                                    selectedAccountEntity = card
                                                    showUpdateDialog = true
                                                },
                                                onViewHistory = {
                                                    historyAccount = card.bankName to card.accountLast4
                                                    manageAccountsViewModel.loadBalanceHistory(
                                                        card.bankName,
                                                        card.accountLast4
                                                    )
                                                    showHistoryDialog = true
                                                },
                                                onAuditBalance = {
                                                    onNavigateToAccountAudit(card.bankName, card.accountLast4)
                                                },
                                                onDeleteAccount = {
                                                    accountToDelete = card
                                                    showDeleteConfirmDialog = true
                                                },
                                                onEditAccount = {
                                                    accountToEdit = card
                                                    showEditSheet = true
                                                },
                                                onAccountClick = {
                                                    onNavigateToAccountDetail(card.bankName, card.accountLast4)
                                                },
                                                onMergeAccount = {
                                                    accountForMerge = card
                                                    showMergeSelection = true
                                                },
                                                onSettleBill = {
                                                    cardToSettle = card
                                                    showSettleSheet = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }

    // Account Diagnostics & Health Sheet
    if (showDiagnosticsSheet) {
        AccountDiagnosticsSheet(
            uiState = uiState,
            onDismiss = { showDiagnosticsSheet = false },
            onUnhideAccount = { bank, last4 ->
                manageAccountsViewModel.toggleAccountVisibility(bank, last4)
            },
            onUnhideAll = {
                manageAccountsViewModel.unhideAllAccounts()
            },
            onMergeAccount = { account ->
                accountForMerge = account
                showMergeSelection = true
                showDiagnosticsSheet = false
            },
            onEditAccount = { account ->
                accountToEdit = account
                showEditSheet = true
                showDiagnosticsSheet = false
            },
            onDeleteAccount = { account ->
                accountToDelete = account
                showDeleteConfirmDialog = true
                showDiagnosticsSheet = false
            },
            onLinkCard = { card ->
                selectedCardForLink = card
                showDiagnosticsSheet = false
            }
        )
    }

    // Update Balance Sheet
    if (showUpdateDialog && selectedAccount != null && selectedAccountEntity != null) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            onDismissRequest = {
                showUpdateDialog = false
                selectedAccount = null
                selectedAccountEntity = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            if (selectedAccountEntity!!.isCreditCard) {
                NumberPad(
                    initialValue =
                        selectedAccountEntity!!.balance.toPlainString(),
                    title = stringResource(R.string.update_outstanding_title),
                    bankName = selectedAccount!!.first,
                    accountLast4 = selectedAccount!!.second,
                    doneButtonLabel = stringResource(R.string.update_outstanding_title),
                    onDone = { newValue ->
                        newValue.toBigDecimalOrNull()?.let { newBalance ->
                            manageAccountsViewModel.updateCreditCard(
                                selectedAccount!!.first,
                                selectedAccount!!.second,
                                newBalance,
                                selectedAccountEntity!!.creditLimit
                                    ?: BigDecimal.ZERO
                            )
                        }
                        showUpdateDialog = false
                        selectedAccount = null
                        selectedAccountEntity = null
                    }
                )
            } else {
                NumberPad(
                    initialValue =
                        selectedAccountEntity!!.balance.toPlainString(),
                    title = stringResource(R.string.update_balance),
                    bankName = selectedAccount!!.first,
                    accountLast4 = selectedAccount!!.second,
                    doneButtonLabel = stringResource(R.string.update_balance),
                    onDone = { newValue ->
                        newValue.toBigDecimalOrNull()?.let { newBalance ->
                            manageAccountsViewModel.updateAccountBalance(
                                selectedAccount!!.first,
                                selectedAccount!!.second,
                                newBalance
                            )
                        }
                        showUpdateDialog = false
                        selectedAccount = null
                        selectedAccountEntity = null
                    }
                )
            }
        }
    }

    // Settle Credit Card Sheet
    if (showSettleSheet && cardToSettle != null) {
        val availableSourceAccounts = uiState.accounts.filter { !it.isCreditCard }
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            onDismissRequest = {
                showSettleSheet = false
                cardToSettle = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            SettleCreditCardSheet(
                creditCard = cardToSettle!!,
                availableSourceAccounts = availableSourceAccounts,
                onDismiss = {
                    showSettleSheet = false
                    cardToSettle = null
                },
                onConfirmSettle = { fromAccount, amount ->
                    manageAccountsViewModel.settleCreditCardBill(
                        creditCard = cardToSettle!!,
                        fromAccount = fromAccount,
                        amount = amount
                    )
                    showSettleSheet = false
                    cardToSettle = null
                }
            )
        }
    }

    // Balance History Sheet
    if (showHistoryDialog && historyAccount != null) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            onDismissRequest = {
                showHistoryDialog = false
                historyAccount = null
                manageAccountsViewModel.clearBalanceHistory()
            },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            HistorySheet(
                bankName = historyAccount!!.first,
                accountLast4 = historyAccount!!.second,
                balanceHistory = uiState.balanceHistory,
                onDeleteBalance = { id ->
                    manageAccountsViewModel.deleteBalanceRecord(
                        id,
                        historyAccount!!.first,
                        historyAccount!!.second
                    )
                },
                onUpdateBalance = { id, newBalance ->
                    manageAccountsViewModel.updateBalanceRecord(
                        id,
                        newBalance,
                        historyAccount!!.first,
                        historyAccount!!.second
                    )
                }
            )
        }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmDialog && accountToDelete != null) {
        DeleteAccountDialog(
            bankName = accountToDelete!!.bankName,
            accountLast4 = accountToDelete!!.accountLast4,
            accountIcon = accountToDelete!!.iconResId,
            accountColor = accountToDelete!!.color,
            isCreditCard = accountToDelete!!.isCreditCard,
            isWallet = accountToDelete!!.isWallet,
            onDismiss = {
                showDeleteConfirmDialog = false
                accountToDelete = null
            },
            onDelete = {
                manageAccountsViewModel.deleteAccount(
                    accountToDelete!!.bankName,
                    accountToDelete!!.accountLast4
                )
                showDeleteConfirmDialog = false
                accountToDelete = null
            },
            hazeState = hazeState,
            blurEffects = blurEffects
        )
    }

    // Deletion Progress Dialog
    if (uiState.isDeletingAccount) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = VittifyShapes.dialog,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "Deleting account...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Edit Account Sheet
    if (showEditSheet && accountToEdit != null) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                showEditSheet = false
                accountToEdit = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            EditAccountSheet(
                account = accountToEdit!!,
                allAccounts = uiState.accounts,
                defaultCurrency = defaultCurrency,
                onDismiss = {
                    showEditSheet = false
                    accountToEdit = null
                },
                onDelete = {
                    accountToDelete = accountToEdit
                    showEditSheet = false
                    accountToEdit = null
                    showDeleteConfirmDialog = true
                },
                onSave = { bankName, balance, last4, iconResId, iconName, color, isCC, isWallet, limit, currency, customId ->
                    manageAccountsViewModel.editAccount(
                        oldBankName = accountToEdit!!.bankName,
                        accountLast4 = accountToEdit!!.accountLast4,
                        newBankName = bankName,
                        newBalance = balance,
                        newCreditLimit = limit,
                        isCreditCard = isCC,
                        isWallet = isWallet,
                        newIconResId = iconResId,
                        newIconName = iconName,
                        newColorHex = color,
                        newCurrency = currency,
                        newCustomId = customId
                    )
                    showEditSheet = false
                    accountToEdit = null
                }
            )
        }
    }

    // Add Account Sheet
    if (showAddSheet) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showAddSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            EditAccountSheet(
                allAccounts = uiState.accounts,
                defaultCurrency = defaultCurrency,
                onDismiss = { showAddSheet = false },
                onSave = { bankName, balance, last4, iconResId, iconName, color, isCC, isWallet, limit, currency, customId ->
                    manageAccountsViewModel.addAccount(
                        bankName = bankName,
                        balance = balance,
                        accountLast4 = last4,
                        iconResId = iconResId,
                        iconName = iconName,
                        colorHex = color,
                        isCreditCard = isCC,
                        isWallet = isWallet,
                        creditLimit = limit,
                        currency = currency,
                        customId = customId
                    )
                    showAddSheet = false
                }
            )
        }
    }

    // Merge Account Dialogs
    if (showMergeSelection && accountForMerge != null) {
        MergeAccountSelectionDialog(
            currentAccount = accountForMerge!!,
            allAccounts = uiState.accounts,
            onDismiss = {
                showMergeSelection = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
            },
            onNext = { accounts ->
                selectedMergeAccounts = accounts
                showMergeSelection = false
                showMergeBalanceOption = true
            }
        )
    }

    if (showMergeBalanceOption && accountForMerge != null && selectedMergeAccounts.isNotEmpty()) {
        MergeBalanceOptionDialog(
            currentAccount = accountForMerge!!,
            selectedAccounts = selectedMergeAccounts,
            onDismiss = {
                showMergeBalanceOption = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
            },
            onOptionSelected = { option ->
                when (option) {
                    BalanceMergeOption.SUM -> {
                        val sumBalance = selectedMergeAccounts.sumOfBigDecimal { acc: AccountBalanceEntity -> acc.balance } + accountForMerge!!.balance
                        mergeNewBalance = sumBalance
                        showMergeBalanceOption = false
                        showMergeConfirmation = true
                    }
                    BalanceMergeOption.MANUAL -> {
                        showMergeBalanceOption = false
                        showMergeManualInput = true
                    }
                    BalanceMergeOption.NONE -> {
                        mergeNewBalance = accountForMerge!!.balance
                        showMergeBalanceOption = false
                        showMergeConfirmation = true
                    }
                }
            }
        )
    }

    if (showMergeManualInput && accountForMerge != null && selectedMergeAccounts.isNotEmpty()) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            onDismissRequest = {
                showMergeManualInput = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            NumberPad(
                initialValue = accountForMerge!!.balance.toPlainString(),
                title = stringResource(R.string.enter_merged_balance),
                bankName = accountForMerge!!.bankName,
                accountLast4 = accountForMerge!!.accountLast4,
                doneButtonLabel = stringResource(R.string.confirm_balance),
                onDone = { newValue ->
                    newValue.toBigDecimalOrNull()?.let { newBalance ->
                        mergeNewBalance = newBalance
                        showMergeManualInput = false
                        showMergeConfirmation = true
                    }
                }
            )
        }
    }

    if (showMergeConfirmation && accountForMerge != null && selectedMergeAccounts.isNotEmpty() && mergeNewBalance != null) {
        MergeConfirmationDialog(
            onDismiss = {
                showMergeConfirmation = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
                mergeNewBalance = null
            },
            onConfirm = {
                manageAccountsViewModel.mergeAccounts(
                    targetAccount = accountForMerge!!,
                    sourceAccounts = selectedMergeAccounts,
                    newBalance = mergeNewBalance!!
                )
                showMergeConfirmation = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
                mergeNewBalance = null
            },
            hazeState = hazeState,
            blurEffects = blurEffects
        )
    }

    if (selectedCardForLink != null) {
        val card = selectedCardForLink!!
        val matchingAccounts = uiState.accounts.filter { it.bankName == card.bankName }
        LinkCardDialog(
            card = card,
            accounts = matchingAccounts,
            onDismiss = { selectedCardForLink = null },
            onConfirm = { accountLast4 ->
                scope.launch {
                    manageAccountsViewModel.linkCardToAccount(card.id, accountLast4)
                    selectedCardForLink = null
                }
            },
            hazeState = hazeState,
            blurEffects = blurEffects
        )
    }
}



@Composable
private fun CreditCardItem(
    card: AccountBalanceEntity,
    position: ListItemPosition = ListItemPosition.Single,
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit,
    onDeleteAccount: () -> Unit,
    isMain: Boolean = false,
    onSetAsMain: () -> Unit = {},
    onEditAccount: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onMergeAccount: () -> Unit = {},
    onAuditBalance: () -> Unit = {},
    onSettleBill: (() -> Unit)? = null
) {
    val available = (card.creditLimit ?: BigDecimal.ZERO) - card.balance
    val utilization =
        if (card.creditLimit != null && card.creditLimit > BigDecimal.ZERO) {
            ((card.balance.toDouble() / card.creditLimit.toDouble()) * 100).toInt()
        } else {
            0
        }

    val utilizationColor =
        when {
            utilization > 70 -> MaterialTheme.colorScheme.error
            utilization > 30 -> Color(0xFFFF9800) // Orange
            else -> Color(0xFF4CAF50) // Green
        }

    AccountCard(
        account = card,
        shape = position.toShape(),
        isHidden = isHidden,
        onUpdateBalance = onUpdateBalance,
        onEditAccount = onEditAccount,
        onViewHistory = onViewHistory,
        onAuditBalance = onAuditBalance,
        onToggleVisibility = onToggleVisibility,
        onDeleteAccount = onDeleteAccount,
        isMain = isMain,
        onSetAsMain = onSetAsMain,
        onClick = onAccountClick,
        onMergeAccount = onMergeAccount,
        onSettleBill = onSettleBill
    ) {
        // Credit Card
        Column(
            modifier = Modifier.padding(horizontal = 16.dp,vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            // Available Credit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.label_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(
                        available,
                        card.currency
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Credit Limit with Utilization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.credit_limit_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = CurrencyFormatter.formatCurrency(
                            card.creditLimit ?: BigDecimal.ZERO,
                            card.currency
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.utilization_used, utilization),
                        style = MaterialTheme.typography.bodySmall,
                        color = utilizationColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeApi::class)
@Composable
private fun AccountItem(
    account: AccountBalanceEntity,
    linkedCards: List<CardEntity> = emptyList(),
    position: ListItemPosition = ListItemPosition.Single,
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit,
    isMain: Boolean = false,
    onSetAsMain: () -> Unit = {},
    onUnlinkCard: (cardId: Long) -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onEditAccount: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onMergeAccount: () -> Unit = {},
    onAuditBalance: () -> Unit = {}
) {
    Column {
        AccountCard(
            account = account,
            shape = position.toShape(),
            isHidden = isHidden,
            onUpdateBalance = onUpdateBalance,
            onEditAccount = onEditAccount,
            onViewHistory = onViewHistory,
            onAuditBalance = onAuditBalance,
            onToggleVisibility = onToggleVisibility,
            onDeleteAccount = onDeleteAccount,
            isMain = isMain,
            onSetAsMain = onSetAsMain,
            onClick = onAccountClick,
            onMergeAccount = onMergeAccount
        ) {

            // Linked Cards Section
            if (linkedCards.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.linked_cards_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                    linkedCards.forEach { card ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.xs),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 2.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "💳",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Column {
                                        Row(modifier = Modifier.padding(start = Spacing.sm),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                        ) {
                                            Text(
                                                text = "**** **** **** ${card.cardLast4}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (!card.isActive
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.label_inactive),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { onUnlinkCard(card.id) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shapes = IconButtonDefaults.shapes(),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LinkOff,
                                        contentDescription = stringResource(R.string.unlink_card_desc),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OrphanedCardItem(
    card: CardEntity,
    accounts: List<AccountBalanceEntity>,
    onLinkToAccount: (String) -> Unit,
    onDeleteCard: (Long) -> Unit
) {
    var expandedSource by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(
            onClick = { expandedSource = !expandedSource },
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ),
        shape = VittifyShapes.platter,
        border = VittifySurface.platterBorder(),
        colors = CardDefaults.cardColors(
            containerColor = VittifySurface.platterContainerColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💳", style = MaterialTheme.typography.titleMedium)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.bankName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "**** **** **** ${card.cardLast4}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = stringResource(R.string.more_options_desc),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = MaterialTheme.shapes.large,
                        containerColor = Color.Transparent,
                        shadowElevation = 0.dp,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.link_to_account)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Link,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onLinkToAccount("")
                            },
                            modifier = Modifier
                                .shadow(
                                    elevation = 2.dp,
                                    shape = VittifyShapes.scaled(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .background(
                                    color = VittifySurface.surfaceContainerColor(),
                                    shape = VittifyShapes.scaled(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(1.5.dp))
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.action_delete),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Iconax.Bag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteCard(card.id)
                            },
                            modifier = Modifier
                                .shadow(
                                    elevation = 2.dp,
                                    shape = VittifyShapes.scaled(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = VittifyShapes.scaled(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                        )
                    }
                }
            }
            Text(
                text = "${if (card.cardType == CardType.CREDIT) stringResource(R.string.credit_card)
                else stringResource(R.string.debit_card)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Show last known balance if available
            if (card.lastBalance != null) {
                Text(
                    text = stringResource(R.string.last_balance, CurrencyFormatter.formatCurrency(card.lastBalance, card.currency)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                )
            }
            // Show source SMS that triggered card detection
            if (card.lastBalanceSource != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.Padding.content)
                        .background(
                            color = VittifySurface.surfaceColor(),
                            shape = VittifyShapes.input
                        )
                        .padding(Dimensions.Padding.content)
                ) {
                    Text(
                        text = if (expandedSource) {
                            stringResource(R.string.sms_source_formatted, card.lastBalanceSource)
                        } else {
                            stringResource(R.string.sms_source_expand, card.lastBalanceSource.take(80))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expandedSource) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkCardDialog(
    card: CardEntity,
    accounts: List<AccountBalanceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState = remember { HazeState() }
) {
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.link_card_title))
                Text(
                    text = "${card.bankName} ••${card.cardLast4}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (accounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_accounts_found_link, card.bankName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = stringResource(R.string.select_account_link_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                    accounts.forEach { account ->
                        val isSelected = selectedAccount == account.accountLast4
                        val tokens = LocalVittifyTokens.current
                        Surface(
                            onClick = { selectedAccount = account.accountLast4 },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = (tokens.surfaceOpacity * 0.95f).coerceIn(0.4f, 1f))
                                   else VittifySurface.surfaceColor(),
                            shape = VittifyShapes.input,
                            border = VittifySurface.platterBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.reddy.vittify.presentation.ui.components.BrandIcon(
                                    merchantName = account.bankName,
                                    accountIconResId = account.iconResId,
                                    accountColorHex = account.color,
                                    size = 32.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "••${account.accountLast4}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(
                                            account.balance,
                                            account.currency
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VittifySurface.surfaceColor(),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = VittifyShapes.scaled(
                            topStart = Dimensions.Radius.xxl,
                            topEnd = Dimensions.Radius.xs,
                            bottomStart = Dimensions.Radius.xxl,
                            bottomEnd = Dimensions.Radius.xs
                        ),
                        modifier = Modifier
                            .padding(start = Spacing.xl)
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Button(
                        onClick = { selectedAccount?.let(onConfirm) },
                        enabled = selectedAccount != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = VittifyShapes.scaled(
                            topStart = Dimensions.Radius.xs,
                            topEnd = Dimensions.Radius.xxl,
                            bottomStart = Dimensions.Radius.xs,
                            bottomEnd = Dimensions.Radius.xxl
                        ),
                        modifier = Modifier
                            .padding(end = Spacing.xl)
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.action_link),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        containerColor = VittifySurface.surfaceContainerLowColor(),
        dismissButton = {},
        shape = VittifyShapes.dialog,
        modifier = Modifier
            .clip(VittifyShapes.dialog)
            .then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = HazeDefaults.tint(containerColor),
                            blurRadius = 20.dp,
                            noiseFactor = -1f,
                        )
                        blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                    }
                ) else Modifier
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleCreditCardSheet(
    creditCard: AccountBalanceEntity,
    availableSourceAccounts: List<AccountBalanceEntity>,
    onDismiss: () -> Unit,
    onConfirmSettle: (fromAccount: AccountBalanceEntity, amount: BigDecimal) -> Unit
) {
    var selectedFromAccount by remember {
        mutableStateOf(availableSourceAccounts.firstOrNull())
    }
    var amountText by remember { mutableStateOf(creditCard.balance.toPlainString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settle Credit Card Bill",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Card details banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.platter,
            border = VittifySurface.platterBorder(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f * LocalVittifyTokens.current.surfaceOpacity)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = creditCard.bankName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "**** **** **** ${creditCard.accountLast4}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(creditCard.balance, creditCard.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Pay From Account Selector
        Text(
            text = "Pay From Account",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        if (availableSourceAccounts.isEmpty()) {
            Text(
                text = "No available bank accounts to pay from.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableSourceAccounts) { account ->
                    val isSelected = selectedFromAccount?.id == account.id
                    Surface(
                        onClick = { selectedFromAccount = account },
                        shape = VittifyShapes.input,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = LocalVittifyTokens.current.surfaceOpacity) else VittifySurface.surfaceContainerLowColor(),
                        border = VittifySurface.platterBorder(),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = account.bankName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (account.isWallet) "Wallet" else "**** ${account.accountLast4}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.formatCurrency(account.balance, account.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Amount Input Field
        Text(
            text = "Settlement Amount",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = amountText,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                    amountText = input
                    errorMessage = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Amount (${creditCard.currency})") },
            trailingIcon = {
                TextButton(
                    onClick = { amountText = creditCard.balance.toPlainString() }
                ) {
                    Text("Full Bill")
                }
            },
            singleLine = true,
            shape = VittifyShapes.input,
            isError = errorMessage != null
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = VittifyShapes.button
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val fromAcc = selectedFromAccount
                    val amt = amountText.toBigDecimalOrNull()
                    if (fromAcc == null) {
                        errorMessage = "Please select a source account"
                    } else if (amt == null || amt <= BigDecimal.ZERO) {
                        errorMessage = "Please enter a valid amount"
                    } else {
                        onConfirmSettle(fromAcc, amt)
                    }
                },
                enabled = selectedFromAccount != null && amountText.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = VittifyShapes.button
            ) {
                Text("Settle Bill")
            }
        }
    }
}


