package com.reddy.vittify.presentation.ui.features.settings.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.reddy.vittify.R
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.features.globalsearch.GlobalSearchViewModel
import com.reddy.vittify.presentation.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import com.reddy.vittify.presentation.navigation.SettingsDeepLink
import com.reddy.vittify.presentation.ui.components.settingOptionHighlight
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    targetOptionId: String? = null,
    globalSearchViewModel: GlobalSearchViewModel = hiltViewModel(),
    blurEffects: Boolean = true
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    var showGlobalSearchSettings by remember { mutableStateOf(false) }

    val bringIntoViewRequesters = remember {
        mapOf(
            "global-search" to BringIntoViewRequester()
        )
    }
    var highlightedOptionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetOptionId) {
        targetOptionId?.let { rawId ->
            val id = SettingsDeepLink.normalizeOptionSlug("customization", rawId)
            if (id == "global-search") {
                showGlobalSearchSettings = true
            }
            delay(300)
            bringIntoViewRequesters[id]?.bringIntoView()
            highlightedOptionId = id
            delay(2500)
            highlightedOptionId = null
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.customization),
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
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                // Global Search Settings Item
                ListItem(
                    modifier = Modifier.settingOptionHighlight(
                        id = "global-search",
                        requester = bringIntoViewRequesters["global-search"],
                        highlightedId = highlightedOptionId,
                        shape = ListItemPosition.Single.toShape()
                    ),
                    headline = {
                        Text(
                            text = stringResource(R.string.global_search_settings),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    supporting = {
                        Text(
                            text = stringResource(R.string.global_search_settings_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = orange_light,
                                    shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.SCALLOP_12.composeShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Iconax.Search,
                                contentDescription = null,
                                tint = orange_dark
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
                    onClick = { showGlobalSearchSettings = true },
                    shape = ListItemPosition.Single.toShape(),
                    padding = PaddingValues(0.dp)
                )
            }
        }

        if (showGlobalSearchSettings) {
            GlobalSearchSettingsBottomSheet(
                viewModel = globalSearchViewModel,
                onDismiss = { showGlobalSearchSettings = false }
            )
        }
    }
}

