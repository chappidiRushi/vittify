package com.reddy.vittify.presentation.ui.features.settings.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.reddy.vittify.R
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionSettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = dev.chrisbanes.haze.HazeState()
    val bottomNavPadding = LocalBottomNavPadding.current

    val useCategoryAsMerchant by viewModel.useCategoryAsMerchant.collectAsState()
    val useCategoryAsMerchantSms by viewModel.useCategoryAsMerchantSms.collectAsState()
    val preserveAccountOrder by viewModel.preserveAccountOrder.collectAsState()
    val directFieldEditing by viewModel.directFieldEditing.collectAsState()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.transaction_settings),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
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
                        bottom = Dimensions.Padding.content + bottomNavPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.transaction_settings_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.xs)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    PreferenceSwitch(
                        title = stringResource(R.string.use_category_as_merchant),
                        subtitle = stringResource(R.string.use_category_as_merchant_desc),
                        checked = useCategoryAsMerchant,
                        onCheckedChange = { viewModel.setUseCategoryAsMerchant(it) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.SCALLOP_12.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        padding = PaddingValues(0.dp),
                        isFirst = true,
                        isLast = false
                    )

                    PreferenceSwitch(
                        title = stringResource(R.string.use_category_as_merchant_sms),
                        subtitle = stringResource(R.string.use_category_as_merchant_sms_desc),
                        checked = useCategoryAsMerchantSms,
                        onCheckedChange = { viewModel.setUseCategoryAsMerchantSms(it) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.COOKIE_8.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Sms,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        },
                        padding = PaddingValues(0.dp),
                        isFirst = false,
                        isLast = false
                    )

                    PreferenceSwitch(
                        title = stringResource(R.string.preserve_account_order),
                        subtitle = stringResource(R.string.preserve_account_order_desc),
                        checked = preserveAccountOrder,
                        onCheckedChange = { viewModel.setPreserveAccountOrder(it) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.PENTAGON.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        },
                        padding = PaddingValues(0.dp),
                        isFirst = false,
                        isLast = false
                    )

                    PreferenceSwitch(
                        title = stringResource(R.string.direct_field_editing),
                        subtitle = stringResource(R.string.direct_field_editing_desc),
                        checked = directFieldEditing,
                        onCheckedChange = { viewModel.setDirectFieldEditing(it) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = com.reddy.vittify.presentation.ui.components.VittifySvgShape.TILTED_PILL.composeShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        padding = PaddingValues(0.dp),
                        isFirst = false,
                        isLast = true
                    )
                }
            }
        }
    }
}
