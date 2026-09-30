package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.toColorInt
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.presentation.common.icons.IconProvider
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySpacing
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback
import com.reddy.vittify.utils.formatBalance
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Material 3 Expressive Bank Cards Carousel.
 * Featuring fluid HorizontalPager snapping, elegant peeking cards,
 * tactile frosted backgrounds, and strong monetary hierarchy.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AccountCarousel(
    modifier: Modifier = Modifier,
    bankAccounts: List<AccountBalanceEntity>,
    creditCards: List<AccountBalanceEntity>,
    onAccountClick: (bankName: String, accountLast4: String) -> Unit = { _, _ -> },
    animatedContentScope: AnimatedVisibilityScope? = null,
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    val totalAccounts = bankAccounts.size + creditCards.size
    val allAccounts = bankAccounts + creditCards

    val isTransitioning = animatedContentScope?.transition?.let {
        it.currentState != it.targetState
    } ?: false

    if (totalAccounts == 0) return

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (totalAccounts == 1) {
            // Single account card: full-width hero card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VittifySpacing.scaledStandard)
            ) {
                val account = allAccounts.first()
                AccountCarouselCard(
                    bankName = account.bankName,
                    accountLast4 = account.accountLast4,
                    balance = account.formatBalance(),
                    subtitle = when {
                        account.isWallet -> "Wallet"
                        creditCards.contains(account) -> "Credit Card"
                        else -> "Savings account"
                    },
                    onClick = { onAccountClick(account.bankName, account.accountLast4) },
                    animatedContentScope = animatedContentScope,
                    isWallet = account.isWallet,
                    iconResId = account.iconResId,
                    iconName = account.iconName,
                    color = account.color,
                    blurEffects = blurEffects,
                    hazeState = hazeState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                )
            }
        } else {
            // Multiple accounts: Centered peeking carousel with tactile spring snapping
            val pagerState = rememberPagerState(pageCount = { allAccounts.size })
            val coroutineScope = rememberCoroutineScope()
            val haptic = rememberAppHapticFeedback()

            val flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp),
                contentPadding = PaddingValues(horizontal = VittifySpacing.scaledStandard),
                pageSpacing = 12.dp,
                flingBehavior = flingBehavior,
                userScrollEnabled = !isTransitioning
            ) { page ->
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                )
                val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
                val scale = lerp(1f, 0.94f, absOffset)
                val alpha = lerp(1f, 0.82f, absOffset)

                val account = allAccounts[page]
                val isCurrentPage = pagerState.currentPage == page

                AccountCarouselCard(
                    bankName = account.bankName,
                    accountLast4 = account.accountLast4,
                    balance = account.formatBalance(),
                    subtitle = when {
                        account.isWallet -> "Wallet"
                        creditCards.contains(account) -> "Credit Card"
                        else -> "Savings account"
                    },
                    onClick = {
                        if (isCurrentPage) {
                            onAccountClick(account.bankName, account.accountLast4)
                        } else {
                            haptic.click()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    },
                    animatedContentScope = animatedContentScope,
                    isWallet = account.isWallet,
                    iconResId = account.iconResId,
                    iconName = account.iconName,
                    color = account.color,
                    blurEffects = blurEffects,
                    hazeState = hazeState,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .fillMaxSize()
                )
            }

            // Material 3 Expressive Animated Pager Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalAccounts) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 20.dp else 6.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "account_pager_dot_width_$index"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "account_pager_dot_color_$index"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(VittifyShapes.pill)
                            .background(dotColor)
                            .clickable {
                                if (pagerState.currentPage != index) {
                                    haptic.click()
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            index,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Expressive Hero Balance Component for Account Cards adhering to GEMINI.md Rule 13:
 * - Currency symbol: smaller and lighter
 * - Major amount: large and bold headline
 * - Decimal/paise: smaller and quieter
 */
@Composable
private fun CardBalanceDisplay(
    balance: String,
    modifier: Modifier = Modifier
) {
    data class ParsedBalance(
        val prefixSymbol: String,
        val major: String,
        val decimal: String,
        val suffixSymbol: String
    )

    val parsed = remember(balance) {
        val trimmed = balance.trim()
        if (trimmed.isEmpty()) {
            return@remember ParsedBalance("", "0", "", "")
        }

        // Find index range of number part (digits and separators)
        var startIndex = 0
        while (startIndex < trimmed.length && !trimmed[startIndex].isDigit()) {
            startIndex++
        }
        var endIndex = trimmed.length
        while (endIndex > startIndex && !trimmed[endIndex - 1].isDigit()) {
            endIndex--
        }

        val prefix = trimmed.substring(0, startIndex).trim()
        val suffix = trimmed.substring(endIndex).trim()
        val numberPart = if (startIndex < endIndex) trimmed.substring(startIndex, endIndex) else trimmed

        // Check if there is a decimal separator (. or ,) followed by 1 or 2 digits at the end
        val decimalRegex = Regex("""([.,])(\d{1,2})$""")
        val match = decimalRegex.find(numberPart)

        if (match != null) {
            val separator = match.groupValues[1]
            val decimalDigits = match.groupValues[2]
            val majorPart = numberPart.substring(0, match.range.first)
            ParsedBalance(
                prefixSymbol = prefix,
                major = majorPart.ifEmpty { "0" },
                decimal = "$separator$decimalDigits",
                suffixSymbol = suffix
            )
        } else {
            ParsedBalance(
                prefixSymbol = prefix,
                major = numberPart.ifEmpty { "0" },
                decimal = "",
                suffixSymbol = suffix
            )
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        if (parsed.prefixSymbol.isNotBlank()) {
            Text(
                text = parsed.prefixSymbol,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 3.dp, end = 4.dp),
                maxLines = 1
            )
        }
        Text(
            text = parsed.major,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (parsed.decimal.isNotBlank()) {
            Text(
                text = parsed.decimal,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 2.dp),
                maxLines = 1
            )
        }
        if (parsed.suffixSymbol.isNotBlank()) {
            Text(
                text = if (parsed.suffixSymbol.startsWith(" ")) parsed.suffixSymbol else " ${parsed.suffixSymbol}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 3.dp),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalHazeApi::class)
@Composable
fun SharedTransitionScope.AccountCarouselCard(
    bankName: String,
    accountLast4: String,
    balance: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animatedContentScope: AnimatedVisibilityScope? = null,
    isWallet: Boolean = false,
    iconResId: Int = 0,
    iconName: String? = null,
    color: String? = null,
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer

    val brandTint = remember(color) {
        try {
            color?.let { Color(it.toColorInt()) } ?: Color.Transparent
        } catch (_: Exception) {
            Color.Transparent
        }
    }

    val haptic = rememberAppHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "account_card_press_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(185.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                onClick = {
                    haptic.click()
                    onClick()
                },
                indication = null,
                interactionSource = interactionSource
            )
            .then(
                if (animatedContentScope != null) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "account_${bankName}_${accountLast4}"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = { _, _ ->
                            spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioNoBouncy
                            )
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )
                    ).skipToLookaheadSize()
                } else Modifier
            )
            .clip(VittifyShapes.hero)
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
            ),
        color = VittifySurface.platterContainerColor(),
        border = VittifySurface.platterBorder() ?: BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = VittifySurface.dividerAlpha)
        ),
        shape = VittifyShapes.hero,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val iconResource = remember(bankName, iconResId, iconName) {
                IconProvider.getIconForTransaction(
                    context = context,
                    merchantName = bankName,
                    accountIconResId = iconResId,
                    accountIconName = iconName
                )
            }

            // Tonal Frosted Gradient Overlay for tactile finish
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                if (brandTint != Color.Transparent) brandTint.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent,
                                if (brandTint != Color.Transparent) brandTint.copy(alpha = 0.05f)
                                else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                            )
                        )
                    )
            )

            // Faint branded icon watermark in background
            TiledScrollingIconBackground(
                iconResource = iconResource,
                opacity = 0.04f,
                iconSize = 56.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(VittifySpacing.scaledComfortable),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Brand Icon Badge + Card/Account Label Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandIcon(
                        merchantName = bankName,
                        size = 42.dp,
                        showBackground = true,
                        accountIconResId = iconResId,
                        accountIconName = iconName,
                        accountColorHex = color
                    )

                    // Account Pill Badge
                    Surface(
                        shape = VittifyShapes.pill,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = if (isWallet) bankName.uppercase() else "${bankName.uppercase()} ••$accountLast4",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Middle: Dedicated Hero Balance spanning the entire width without choking
                CardBalanceDisplay(
                    balance = balance,
                    modifier = Modifier.fillMaxWidth()
                )

                // Bottom Row: Subtitle on left, "View details ›" button on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Interactive "View details ›" Pill Button
                    Surface(
                        onClick = {
                            haptic.click()
                            onClick()
                        },
                        shape = VittifyShapes.pill,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "View details",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

