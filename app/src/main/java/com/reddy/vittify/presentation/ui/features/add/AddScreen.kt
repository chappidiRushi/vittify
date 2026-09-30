package com.reddy.vittify.presentation.ui.features.add

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.GenericTypeSwitcher
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.data.database.entity.TransactionType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AddScreen(
    addViewModel: AddViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialTab: Int = 0,
    subscriptionId: Long? = null,
    transactionType: String? = null,
    // NLP Quick Add prefill fields (all optional)
    nlpAmount: String? = null,
    nlpMerchant: String? = null,
    nlpType: String? = null,
    nlpBankName: String? = null,
    nlpNotes: String? = null,
    nlpCategory: String? = null,
    nlpSubcategory: String? = null,
    onNavigateToTransactionSettings: () -> Unit = {},
    blurEffects: Boolean,
) {
    val hazeState = remember { HazeState() }
    val pagerState = rememberPagerState(
        initialPage = initialTab,
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()

    // Reset state when screen is opened to avoid stale data from previous entries.
    // Skip reset when NLP prefill fields are present so they are not wiped.
    val hasNlpData = nlpAmount != null || nlpMerchant != null || nlpType != null || nlpCategory != null || nlpSubcategory != null
    val accounts by addViewModel.accounts.collectAsState()

    LaunchedEffect(subscriptionId, transactionType, nlpAmount, nlpMerchant, nlpType, nlpCategory, nlpSubcategory) {
        if (subscriptionId == null && transactionType == null && !hasNlpData) {
            addViewModel.resetAllStates()
        }

        if (subscriptionId != null) {
            addViewModel.loadSubscriptionForEdit(subscriptionId)
        }

        if (transactionType != null) {
            try {
                val type = TransactionType.valueOf(transactionType)
                addViewModel.updateTransactionType(type)
            } catch (e: Exception) {
                // Ignore invalid types
            }
        }

        // Apply NLP prefill if any field is present
        if (hasNlpData) {
            val nlpTxType = nlpType?.let {
                try { TransactionType.valueOf(it) } catch (_: Exception) { TransactionType.EXPENSE }
            } ?: TransactionType.EXPENSE

            val draft = com.reddy.vittify.data.nlp.ParsedTransactionDraft(
                amount = nlpAmount ?: "",
                merchant = nlpMerchant ?: "",
                type = nlpTxType,
                bankName = nlpBankName ?: "",
                notes = nlpNotes ?: "",
                category = nlpCategory ?: "Miscellaneous",
                subcategory = nlpSubcategory ?: ""
            )
            addViewModel.prefillFromNlpDraft(draft)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    
    // Track if a transition is currently running to prevent race conditions in UI interaction
    val isTransitioning = animatedVisibilityScope.transition.let {
        it.currentState != it.targetState
    }

    val tabs = listOf(
        stringResource(R.string.transaction),
        stringResource(R.string.subscription)
    )

    Box(
        modifier =
            Modifier.sharedBounds(
                rememberSharedContentState(key = "fab_to_add"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                    contentScale = ContentScale.FillBounds,
                    alignment = Alignment.Center
                )
            )
                .skipToLookaheadSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CustomTitleTopAppBar(
                    scrollBehaviorSmall = scrollBehaviorSmall,
                    scrollBehaviorLarge = scrollBehavior,
                    title = stringResource(R.string.add_new),
                    hazeState = hazeState,
                    hasBackButton = true,
                    hasActionButton = true,
                    actionContent = {
                        IconButton(onClick = { if (!isTransitioning) onNavigateToTransactionSettings() }) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.transaction_settings),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationContent = { NavigationContent { if (!isTransitioning) onNavigateBack() } }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content +
                                paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Type Switcher
                GenericTypeSwitcher(
                    selectedIndex = pagerState.currentPage,
                    onIndexChange = { index ->
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }},
                    options = tabs,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg)
                )



                // Tab Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    userScrollEnabled = !isTransitioning
                ) { page ->
                    when (page) {
                        0 -> TransactionTabContent(
                            viewModel = addViewModel,
                            onSave = onNavigateBack,
                            isTransitioning = isTransitioning,
                            blurEffects = blurEffects,
                            hazeState = hazeState
                        )
                        1 -> SubscriptionTabContent(
                            viewModel = addViewModel,
                            onSave = onNavigateBack,
                            isTransitioning = isTransitioning,
                            blurEffects = blurEffects,
                            hazeState = hazeState
                        )
                    }
                }
            }
        }
    }
}
