package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.R
import com.reddy.vittify.presentation.effects.BlurredAnimatedVisibility
import com.reddy.vittify.presentation.ui.theme.LocalBlurEffects
import com.reddy.vittify.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeDefaults.tint
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CustomTitleTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehaviorSmall: TopAppBarScrollBehavior,
    scrollBehaviorLarge: TopAppBarScrollBehavior,
    title: String,
    hasBackButton: Boolean = false,
    hasActionButton: Boolean = false,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    extraInfoCard: @Composable () -> Unit = {},
    searchBar: (@Composable (isCollapsed: Boolean) -> Unit)? = null,
    hazeState: HazeState = HazeState(),
    blurEffects: Boolean = LocalBlurEffects.current
) {
    val vittifyTitle = stringResource(R.string.vittify_title)
    val isHomeScreen = title == "Vittify" || title == vittifyTitle
    if (isHomeScreen && searchBar != null) {
        HomeStickyCollapsingTopAppBar(
            modifier = modifier,
            scrollBehavior = scrollBehaviorLarge,
            navigationContent = navigationContent,
            extraInfoCard = extraInfoCard,
            searchBar = searchBar,
            hazeState = hazeState,
            blurEffects = blurEffects
        )
        return
    }

    val collapsedFraction = scrollBehaviorLarge.state.collapsedFraction

    // LargeTopAppBar
    if(scrollBehaviorLarge != scrollBehaviorSmall) {
        LargerTopAppBar(
            scrollBehaviorLarge = scrollBehaviorLarge,
            title = title,
            hasBackButton = hasBackButton,
            collapsedFraction = collapsedFraction,
            actionContent = actionContent,
            navigationContent = navigationContent,
            extraInfoCard = extraInfoCard,
            searchBar = searchBar,
            hazeState = hazeState,
            blurEffects = blurEffects,
            themeColors = MaterialTheme.colorScheme
        )
    }

    // Regular TopAppBar
    RegularTopAppBar(
        scrollBehaviorSmall = scrollBehaviorSmall,
        title = title,
        hasBackButton = hasBackButton,
        hasActionButton = hasActionButton,
        actionContent = actionContent,
        navigationContent = navigationContent,
        searchBar = searchBar,
        collapsedFraction = if(scrollBehaviorLarge != scrollBehaviorSmall)collapsedFraction else 1f,
        modifier = modifier,
        hazeState = hazeState,
        blurEffects = blurEffects
    )

}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
private fun HomeStickyCollapsingTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    navigationContent: @Composable () -> Unit,
    extraInfoCard: @Composable () -> Unit,
    searchBar: @Composable (isCollapsed: Boolean) -> Unit,
    hazeState: HazeState,
    blurEffects: Boolean = true
) {
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val collapsedContentHeight = 64.dp
    val expandedContentHeight = 132.dp
    val collapsibleDistance = expandedContentHeight - collapsedContentHeight

    val collapsibleDistancePx = with(density) { -collapsibleDistance.toPx() }

    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != collapsibleDistancePx) {
            scrollBehavior.state.heightOffsetLimit = collapsibleDistancePx
        }
    }

    val fraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    val currentContentHeight = lerp(expandedContentHeight, collapsedContentHeight, fraction)
    val totalHeight = statusBarTop + currentContentHeight

    // Background styling with Haze
    val surfaceColor = MaterialTheme.colorScheme.surface
    val bgAlpha = lerpFloat(0f, 0.95f, (fraction * 1.5f).coerceIn(0f, 1f))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .then(
                if (blurEffects && fraction > 0.01f) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                blurRadius = lerp(0.dp, 20.dp, fraction),
                                noiseFactor = -1f,
                            )
                            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                        }
                    )
                } else Modifier
            )
            .background(surfaceColor.copy(alpha = bgAlpha))
            .padding(top = statusBarTop)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val totalWidth = maxWidth

            // 1. Profile Avatar
            // Smoothly scales from 48dp to 40dp and stays anchored at x = 16dp
            val avatarSize = lerp(48.dp, 40.dp, fraction)
            val avatarX = 16.dp
            val avatarY = lerp(6.dp, 12.dp, fraction)

            Box(
                modifier = Modifier
                    .offset { IntOffset(avatarX.roundToPx(), avatarY.roundToPx()) }
                    .size(avatarSize),
                contentAlignment = Alignment.Center
            ) {
                navigationContent()
            }

            // 2. Greeting Content (Text + Notification Icon)
            // Starts at x = 76dp, fades out gracefully as user scrolls
            val greetingAlpha = (1f - fraction * 2.5f).coerceIn(0f, 1f)
            if (greetingAlpha > 0.01f) {
                val greetingY = lerp(6.dp, -16.dp, fraction)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(76.dp.roundToPx(), greetingY.roundToPx()) }
                        .width((totalWidth - 76.dp - 16.dp).coerceAtLeast(0.dp))
                        .height(52.dp)
                        .alpha(greetingAlpha)
                ) {
                    extraInfoCard()
                }
            }

            // 3. Search Bar
            // Glides diagonally from (16dp, 66dp) to (66dp, 10dp), docking right next to avatar
            val searchStartX = lerp(16.dp, 66.dp, fraction)
            val searchY = lerp(66.dp, 10.dp, fraction)
            val searchWidth = (totalWidth - searchStartX - 16.dp).coerceAtLeast(0.dp)
            val searchHeight = lerp(48.dp, 44.dp, fraction)
            val isFullyCollapsed = fraction >= 0.85f

            Box(
                modifier = Modifier
                    .offset { IntOffset(searchStartX.roundToPx(), searchY.roundToPx()) }
                    .width(searchWidth)
                    .height(searchHeight)
            ) {
                searchBar(isFullyCollapsed)
            }
        }
    }
}


@Composable
private fun Modifier.animatedOffsetModifier(
    hasBackButton: Boolean,
    hasActionButton: Boolean = false,
    isHomeScreen: Boolean = false,
): Modifier {
    // Define the target offset based on conditions
    val targetOffsetX = when {
        hasBackButton && hasActionButton-> 0.dp
        isHomeScreen-> (0).dp
        hasBackButton -> (-26).dp
        else -> (-10).dp
    }

    // Convert to pixels for animation
    val density = LocalDensity.current
    val targetOffsetXPx = with(density) { targetOffsetX.toPx() }

    val transition = updateTransition(
        targetState = Triple(hasBackButton, false, targetOffsetXPx), // false for isInSelectionMode
        label = "offsetTransition"
    )

    val animatedOffsetX by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "offsetX"
    ) { (_, _, offset) -> offset }

    // Apply offset directly as a float value instead of rounding to Int
    return this
        .fillMaxWidth()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                // Use the exact float value for positioning
                placeable.placeRelative(x = animatedOffsetX.toInt(), y = 0)
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
private fun LargerTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehaviorLarge: TopAppBarScrollBehavior,
    title: String,
    hasBackButton: Boolean = false,
    collapsedFraction: Float,
    extraInfoCard: @Composable () -> Unit = {},
    searchBar: (@Composable (isCollapsed: Boolean) -> Unit)? = null,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    hazeState: HazeState,
    blurEffects: Boolean = true,
    themeColors: ColorScheme,

    ){
    LargeTopAppBar(
        title = {
            TitleForLargeTopAppBar(
                title = title,
                modifier = modifier ,
                extraInfoCard = extraInfoCard,
                searchBar = searchBar,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor =  Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        navigationIcon = {
            NavigationForLargeTopAppBar(
                hasBackButton = hasBackButton,
                navigationContent = navigationContent,
                isHomeScreen = title == "Vittify"
            )
        },
        actions = {
            ActionForLargeTopAppBar(
                actionContent = actionContent,
                isHomeScreen = title == "Vittify"
            )
        },
        collapsedHeight = TopAppBarDefaults.LargeAppBarCollapsedHeight,
        expandedHeight = if (title == "Vittify") {
            if (searchBar != null) 215.dp else 150.dp
        } else 110.dp,
        windowInsets = WindowInsets(0.dp),
        scrollBehavior = scrollBehaviorLarge,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = tint(backgroundColor),
                            blurRadius = 10.dp,
                            noiseFactor = -1f,
                        )
                        progressive =
                            HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                    }
                ) else Modifier
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .alpha(1f - collapsedFraction)
    )
}

@Composable
private fun TitleForLargeTopAppBar(
    modifier: Modifier = Modifier,
    title: String,
    extraInfoCard: @Composable () -> Unit = {},
    searchBar: (@Composable (isCollapsed: Boolean) -> Unit)? = null,
){
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        BlurredAnimatedVisibility(
            visible = title != "Vittify",
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp)
            )
        }
        extraInfoCard()
        if (searchBar != null) {
            searchBar(false)
        }
    }
}

@Composable
private fun NavigationForLargeTopAppBar(
    hasBackButton: Boolean = false,
    isHomeScreen: Boolean = false,
    navigationContent: @Composable () -> Unit = {},
){
    BlurredAnimatedVisibility(
        visible = hasBackButton && !isHomeScreen,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        navigationContent()
    }
}

@Composable
private fun ActionForLargeTopAppBar(
    actionContent: @Composable () -> Unit = {},
    isHomeScreen: Boolean = false,
){
    BlurredAnimatedVisibility(
        visible = !isHomeScreen,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        actionContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
private fun RegularTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehaviorSmall: TopAppBarScrollBehavior,
    title: String,
    hasBackButton: Boolean = false,
    hasActionButton: Boolean = false,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    searchBar: (@Composable (isCollapsed: Boolean) -> Unit)? = null,
    collapsedFraction: Float,
    hazeState: HazeState,
    blurEffects: Boolean = true
){
    BlurredAnimatedVisibility(
        visible = collapsedFraction > 0.01f,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val isHomeScreen = title == "Vittify"

        TopAppBar(
            title = {
                if (searchBar != null && isHomeScreen) {
                    searchBar(true)
                } else {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.animatedOffsetModifier(
                            hasBackButton = hasBackButton,
                            hasActionButton = hasActionButton,
                            isHomeScreen = isHomeScreen,
                        )
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            navigationIcon = {
                BlurredAnimatedVisibility(
                    visible = hasBackButton || isHomeScreen,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    navigationContent()
                }
            },
            actions = {
                actionContent()
            },
            scrollBehavior = scrollBehaviorSmall,
            windowInsets = WindowInsets(0.dp),
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                blurRadius = 10.dp,
                                noiseFactor = -1f,
                            )
                            progressive =
                                HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                        }
                    ) else Modifier
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .alpha(collapsedFraction)
        )
    }
}

