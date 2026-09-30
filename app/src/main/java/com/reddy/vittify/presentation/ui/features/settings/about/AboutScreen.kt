package com.reddy.vittify.presentation.ui.features.settings.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.reddy.vittify.BuildConfig
import com.reddy.vittify.R
import com.reddy.vittify.core.Constants
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.VittifySvgShape
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.features.settings.SettingsViewModel
import com.reddy.vittify.presentation.ui.icons.*
import com.reddy.vittify.presentation.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToDeveloper: () -> Unit = {},
    onNavigateToFaq: () -> Unit,
    onNavigateToGuides: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTermsOfService: () -> Unit,
    onNavigateToCredits: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    blurEffects: Boolean
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.about),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .overScrollVertical()
                .verticalScroll(
                    state = scrollState,
                    flingBehavior = rememberOverscrollFlingBehavior { scrollState }
                )
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = Dimensions.Padding.content + paddingValues.calculateBottomPadding() + LocalBottomNavPadding.current
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // App Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.Padding.card),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = VittifyShapes.hero
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            shape = VittifyShapes.hero
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vittify),
                        contentDescription = stringResource(R.string.vittify_logo_cd),
                        modifier = Modifier.size(76.dp)
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.Padding.content))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME, settingsViewModel.databaseVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )
            }

            // Developer Item
            AboutDeveloperItem(
                title = stringResource(R.string.developed_by),
                subtitle = "thegodscode@gmail.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:thegodscode@gmail.com".toUri()
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback for Vittify")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                },
                position = ListItemPosition.Single,
                leading = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = VittifySvgShape.COOKIE_8.composeShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Iconax.CodeCircle,
                            contentDescription = stringResource(R.string.lead_developer_cd),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
            )

            // Connect Section (Website and Discord disabled for now; GitHub to current repo)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                AboutListItem(
                    title = stringResource(R.string.github),
                    subtitle = "chappidiRushi/vittify",
                    icon = Iconax.Github,
                    iconColor = green_dark,
                    iconBackground = green_light,
                    iconShape = VittifySvgShape.SCALLOP_12.composeShape,
                    isLink = true,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Constants.Links.GITHUB_URL.toUri()
                        )
                        context.startActivity(intent)
                    },
                    position = ListItemPosition.Single
                )
            }

            // Help & Resources Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                AboutListItem(
                    title = stringResource(R.string.faq),
                    subtitle = stringResource(R.string.faq_desc),
                    icon = Iconax.MessageQuestion,
                    iconColor = orange_dark,
                    iconBackground = orange_light,
                    iconShape = VittifySvgShape.PENTAGON.composeShape,
                    onClick = onNavigateToFaq,
                    position = ListItemPosition.Top
                )

                AboutListItem(
                    title = stringResource(R.string.guides),
                    subtitle = stringResource(R.string.guides_desc),
                    icon = Iconax.DocumentText2,
                    iconColor = purple_dark,
                    iconBackground = purple_light,
                    iconShape = VittifySvgShape.TILTED_PILL.composeShape,
                    onClick = onNavigateToGuides,
                    position = ListItemPosition.Middle
                )

                AboutListItem(
                    title = stringResource(R.string.report_bug),
                    subtitle = stringResource(R.string.report_bug_desc),
                    icon = Iconax.Ghost,
                    iconColor = red_dark,
                    iconBackground = red_light,
                    iconShape = VittifySvgShape.STAR_8.composeShape,
                    isLink = true,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Constants.Links.REPORT_BUG_URL.toUri()
                        )
                        context.startActivity(intent)
                    },
                    position = ListItemPosition.Bottom
                )
            }

            // Legal & About Details Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                AboutListItem(
                    title = stringResource(R.string.privacy_policy),
                    subtitle = stringResource(R.string.privacy_policy_desc),
                    icon = Iconax.SecuritySafe,
                    iconColor = green_dark,
                    iconBackground = green_light,
                    iconShape = VittifySvgShape.CLOVER_4.composeShape,
                    onClick = onNavigateToPrivacyPolicy,
                    position = ListItemPosition.Top
                )

                AboutListItem(
                    title = stringResource(R.string.terms_of_service),
                    subtitle = stringResource(R.string.terms_of_service_desc),
                    icon = Iconax.DocumentText2,
                    iconColor = cyan_dark,
                    iconBackground = cyan_light,
                    iconShape = VittifySvgShape.TILTED_OVAL.composeShape,
                    onClick = onNavigateToTermsOfService,
                    position = ListItemPosition.Middle
                )

                AboutListItem(
                    title = stringResource(R.string.licenses),
                    subtitle = stringResource(R.string.licenses_desc),
                    icon = Iconax.Status,
                    iconColor = orange_dark,
                    iconBackground = orange_light,
                    iconShape = VittifySvgShape.SCALLOP_12.composeShape,
                    onClick = onNavigateToLicenses,
                    position = ListItemPosition.Middle
                )

                AboutListItem(
                    title = stringResource(R.string.credits),
                    subtitle = stringResource(R.string.credits_desc),
                    icon = Icons.Rounded.Favorite,
                    iconColor = red_dark,
                    iconBackground = red_light,
                    iconShape = VittifySvgShape.COOKIE_8.composeShape,
                    onClick = onNavigateToCredits,
                    position = ListItemPosition.Bottom
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.Padding.card))

            Text(
                text = stringResource(R.string.built_with_love),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            )
        }
    }
}

@Composable
fun AboutListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    iconShape: Shape = CircleShape,
    onClick: () -> Unit,
    isLink: Boolean = false,
    position: ListItemPosition = ListItemPosition.Middle,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headline = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        },
        supporting = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leading = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconBackground,
                        shape = iconShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        trailing = {
            Icon(
                if (isLink) Iconax.ExportArrow02 else Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onClick = onClick,
        shape = position.toShape(),
        padding = PaddingValues(0.dp)
    )
}

@Composable
fun AboutDeveloperItem(
    title: String,
    subtitle: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    position: ListItemPosition = ListItemPosition.Middle,
) {
    ListItem(
        headline = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        supporting = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
            )
        },
        leading = leading,
        trailing = {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onClick = onClick,
        shape = position.toShape(),
        padding = PaddingValues(0.dp),
        listColor = MaterialTheme.colorScheme.primaryContainer
    )
}

