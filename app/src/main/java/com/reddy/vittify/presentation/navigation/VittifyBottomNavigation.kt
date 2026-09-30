package com.reddy.vittify.presentation.navigation

import android.view.HapticFeedbackConstants
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.More
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.text.style.TextOverflow
import com.reddy.vittify.data.sync.ViewMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import com.reddy.vittify.presentation.ui.theme.LocalVittifyTokens
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.reddy.vittify.data.preferences.NavigationBarStyle
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.layout.ContentScale
import com.reddy.vittify.presentation.effects.BlurredAnimatedVisibility
import com.reddy.vittify.presentation.ui.components.animatedIconClick
import com.reddy.vittify.presentation.ui.components.animatedIconSelection
import com.reddy.vittify.presentation.ui.components.springPress
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeDefaults.tint
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

data class FabConfig(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit = {},
    val dropdownContent: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null
)

/**
 * Bottom navigation bar component that supports both NORMAL and FLOATING styles.
 * Used in flat navigation structure where all screens are at the same NavHost level.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun VittifyBottomNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    currentDestination: NavDestination?,
    navigationBarStyle: NavigationBarStyle,
    hideLabels: Boolean,
    hidePill: Boolean,
    blurEffects: Boolean,
    visible: Boolean,
    hazeState: HazeState = remember { HazeState() },
    fabConfig: FabConfig? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    isCoupleTrackingEnabled: Boolean = false,
    profileSwitcherInFooter: Boolean = false,
    activeViewMode: ViewMode = ViewMode.PERSONAL,
    partnerName: String? = null,
    userName: String? = null,
    onViewModeSelected: (ViewMode) -> Unit = {}
) {
    val navigationItems = listOf(BottomNavItem.Home, BottomNavItem.Analytics, BottomNavItem.Transactions)
    val effectiveUserName = userName?.ifBlank { null }?.takeIf { it != "User" } ?: "Me"
    val effectivePartnerName = partnerName?.ifBlank { null } ?: "Partner"
    val (profileLabel, profileIcon) = when (activeViewMode) {
        ViewMode.PERSONAL -> effectiveUserName to Icons.Rounded.Person
        ViewMode.COMBINED -> "Both" to Icons.Rounded.Favorite
        ViewMode.PARTNER -> effectivePartnerName to Icons.Rounded.Favorite
    }

    val normalNavItems: List<BottomNavItem?> = if (profileSwitcherInFooter && isCoupleTrackingEnabled) {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Analytics,
            BottomNavItem.Transactions,
            null, // Placeholder for profile switcher
            BottomNavItem.Settings
        )
    } else {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Analytics,
            BottomNavItem.Transactions,
            BottomNavItem.Settings
        )
    }

    val containerColor = MaterialTheme.colorScheme.surface
    val view = LocalView.current
    val isHomeScreen = currentDestination?.hierarchy?.any {
        it.route?.contains(Home::class.qualifiedName ?: "") == true
    } == true

    Box(modifier = modifier) {
        // NORMAL style NavigationBar
        BlurredAnimatedVisibility(
            visible = visible && navigationBarStyle == NavigationBarStyle.NORMAL,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f)
                )
                val tokens = LocalVittifyTokens.current
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha = if (blurEffects) (0.6f * (tokens.surfaceOpacity / 0.85f)).coerceIn(0.2f, 1f) else tokens.surfaceOpacity
                    ),
                    tonalElevation = 2.dp,
                    modifier = Modifier.then(
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
                ) {
                    normalNavItems.forEach { item ->
                        if (item == null) {
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    val nextMode = when (activeViewMode) {
                                        ViewMode.PERSONAL -> ViewMode.COMBINED
                                        ViewMode.COMBINED -> ViewMode.PARTNER
                                        ViewMode.PARTNER -> ViewMode.PERSONAL
                                    }
                                    onViewModeSelected(nextMode)
                                },
                                icon = {
                                    Icon(
                                        imageVector = profileIcon,
                                        contentDescription = profileLabel,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .size(
                                                if (hidePill && hideLabels) 28.dp else 24.dp
                                            )
                                            .animatedIconClick()
                                    )
                                },
                                label = if (hideLabels) null else {
                                    {
                                        Text(
                                            text = profileLabel,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            val selected = if (item == BottomNavItem.Settings) {
                                currentDestination?.hierarchy?.any { dest ->
                                    dest.route?.contains(item.destinationType.qualifiedName ?: "") == true ||
                                    SETTINGS_ROUTES.any { routeName -> dest.route?.contains(routeName ?: "") == true }
                                } == true
                            } else {
                                currentDestination?.hierarchy?.any {
                                    it.route?.contains(item.destinationType.qualifiedName ?: "") == true
                                } == true
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    val startDestId = navController.graph.findStartDestination().id
                                    if (item.destination == Home) {
                                        navController.popBackStack(Home, inclusive = false, saveState = true)
                                    } else if (item == BottomNavItem.Settings) {
                                        val isAlreadyInSettings = currentDestination?.hierarchy?.any { dest ->
                                            dest.route?.contains(item.destinationType.qualifiedName ?: "") == true ||
                                            SETTINGS_ROUTES.any { routeName -> dest.route?.contains(routeName ?: "") == true }
                                        } == true
                                        if (isAlreadyInSettings) {
                                            navController.safeNavigate(item.destination) {
                                                popUpTo(item.destinationType) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        } else {
                                            navController.safeNavigate(item.destination) {
                                                popUpTo(startDestId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    } else if (selected) {
                                        navController.safeNavigate(item.destination) {
                                            popUpTo(item.destinationType) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        navController.safeNavigate(item.destination) {
                                            popUpTo(startDestId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = stringResource(item.titleRes),
                                        tint = if (selected) {
                                            if (hidePill) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onPrimaryContainer
                                        } else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(
                                                if (hidePill && hideLabels) 28.dp else 24.dp
                                            )
                                            .animatedIconSelection(isSelected = selected)
                                            .animatedIconClick()
                                    )
                                },
                                label = if (hideLabels) null else {
                                    {
                                        Text(
                                            text = stringResource(item.titleRes),
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = if (hidePill) Color.Transparent
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // FLOATING style HorizontalFloatingToolbar
        BlurredAnimatedVisibility(
            visible = visible && navigationBarStyle == NavigationBarStyle.FLOATING,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val animScope = this
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tokens = LocalVittifyTokens.current
                    HorizontalFloatingToolbar(
                        modifier = Modifier
                            .clip(FloatingToolbarDefaults.ContainerShape)
                            .then(
                                if (blurEffects) Modifier.hazeEffect(
                                    state = hazeState,
                                    block = fun HazeEffectScope.() {
                                        style = HazeDefaults.style(
                                            backgroundColor = Color.Transparent,
                                            blurRadius = 20.dp,
                                            noiseFactor = -1f,
                                        )
                                        blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                                    }
                                ) else Modifier
                            )
                            .zIndex(1000f)
                            .animateContentSize(
                                MaterialTheme.motionScheme.fastSpatialSpec()
                            )
                            .then(
                                VittifySurface.platterBorder()?.let { Modifier.border(it, CircleShape) } ?: Modifier
                            ),
                        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(
                                alpha = if (blurEffects) (0.75f * (tokens.surfaceOpacity / 0.85f)).coerceIn(0.3f, 1f) else tokens.surfaceOpacity
                            ),
                        ),
                        expanded = true,
                    ) {
                        navigationItems.forEachIndexed { index, item ->
                            if (index == 2) {
                                val sharedBoundsModifier = if (sharedTransitionScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedBounds(
                                            rememberSharedContentState(key = "fab_to_add"),
                                            animatedVisibilityScope = animScope,
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
                                        ).skipToLookaheadSize()
                                    }
                                } else Modifier

                                TonalToggleButton(
                                    checked = false,
                                    onCheckedChange = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        navController.safeNavigate(AddTransaction(initialTab = 0))
                                    },
                                    shapes = ToggleButtonDefaults.shapes(
                                        shape = FloatingToolbarDefaults.ContainerShape,
                                        checkedShape = VittifyShapes.scaled(30.dp)
                                    ),
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .then(sharedBoundsModifier)
                                        .springPress(targetScale = 0.94f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = stringResource(R.string.add_transaction),
                                        modifier = Modifier.animatedIconClick()
                                    )
                                }
                            }

                            val selected = currentDestination?.hierarchy?.any {
                                it.route?.contains(item.destinationType.qualifiedName ?: "") == true
                            } == true

                            TonalToggleButton(
                                checked = selected,
                                onCheckedChange = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    val startDestId = navController.graph.findStartDestination().id
                                    if (item.destination == Home) {
                                        navController.popBackStack(Home, inclusive = false, saveState = true)
                                    } else if (selected) {
                                        navController.safeNavigate(item.destination) {
                                            popUpTo(item.destinationType) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        navController.safeNavigate(item.destination) {
                                            popUpTo(startDestId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                shapes = ToggleButtonDefaults.shapes(
                                    shape = FloatingToolbarDefaults.ContainerShape,
                                    checkedShape = VittifyShapes.scaled(30.dp)
                                ),
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = if(blurEffects)
                                        MaterialTheme.colorScheme.surfaceBright.copy(0.6f)
                                    else MaterialTheme.colorScheme.surfaceBright,
                                    contentColor = MaterialTheme.colorScheme.inverseSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceBright.copy(0.7f),
                                    disabledContentColor = MaterialTheme.colorScheme.inverseSurface.copy(0.5f),
                                    checkedContainerColor =  MaterialTheme.colorScheme.tertiaryContainer,
                                    checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .springPress(targetScale = 0.94f)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.titleRes),
                                    modifier = Modifier
                                        .animatedIconSelection(isSelected = selected)
                                        .animatedIconClick()
                                )
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = fadeIn() + expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()),
                                    exit = fadeOut() + shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec())
                                ) {
                                    Text(
                                        text = stringResource(item.titleRes),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        if (profileSwitcherInFooter && isCoupleTrackingEnabled) {
                            TonalToggleButton(
                                checked = false,
                                onCheckedChange = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    val nextMode = when (activeViewMode) {
                                        ViewMode.PERSONAL -> ViewMode.COMBINED
                                        ViewMode.COMBINED -> ViewMode.PARTNER
                                        ViewMode.PARTNER -> ViewMode.PERSONAL
                                    }
                                    onViewModeSelected(nextMode)
                                },
                                shapes = ToggleButtonDefaults.shapes(
                                    shape = FloatingToolbarDefaults.ContainerShape,
                                    checkedShape = VittifyShapes.scaled(30.dp)
                                ),
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = if (blurEffects)
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(0.7f)
                                    else MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .springPress(targetScale = 0.94f)
                            ) {
                                Icon(
                                    imageVector = profileIcon,
                                    contentDescription = profileLabel,
                                    modifier = Modifier.animatedIconClick()
                                )
                                Text(
                                    text = profileLabel,
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    BlurredAnimatedVisibility(
                        visible = fabConfig != null,
                        enter = fadeIn() + expandHorizontally(
                            expandFrom = Alignment.Start,
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                        ),
                        exit = fadeOut() + shrinkHorizontally(
                            shrinkTowards = Alignment.Start,
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                        )
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        val capturedDropdown = remember(expanded) { fabConfig?.dropdownContent }

                        Box(contentAlignment = Alignment.BottomEnd) {
                            FloatingActionButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    if (fabConfig?.dropdownContent != null) {
                                        expanded = !expanded
                                    } else {
                                        fabConfig?.onClick()
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(50.dp)
                                    .springPress(targetScale = 0.94f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreHoriz,
                                    contentDescription = fabConfig?.contentDescription,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            val dropContainerColor = VittifySurface.surfaceContainerHighColor()
                            val dropShape = VittifyShapes.platter
                            val dropBorder = VittifySurface.platterBorder()
                            if (capturedDropdown != null) {
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier
                                        .clip(dropShape)
                                        .then(if (dropBorder != null) Modifier.border(dropBorder, dropShape) else Modifier)
                                        .then(
                                        if (blurEffects) Modifier.hazeEffect(
                                            state = hazeState,
                                            block = fun HazeEffectScope.() {
                                                style = HazeDefaults.style(
                                                    backgroundColor = Color.Transparent,
                                                    tint = HazeTint(dropContainerColor.copy(0.5f)),
                                                    blurRadius = 36.dp,
                                                    noiseFactor = -1f,
                                                )
                                                blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                                            }
                                        ) else Modifier
                                    ),
                                    containerColor = dropContainerColor,
                                    shape = dropShape
                                ) {
                                    capturedDropdown.invoke(this) { expanded = false }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
