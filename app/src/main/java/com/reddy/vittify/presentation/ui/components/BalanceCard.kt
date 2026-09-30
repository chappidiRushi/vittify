package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import com.reddy.vittify.presentation.effects.BlurredAnimatedVisibility
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.LongArrow
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySpacing
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.success_dark
import com.reddy.vittify.presentation.ui.theme.expense_dark
import com.reddy.vittify.utils.CurrencyFormatter
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.math.BigDecimal

@OptIn(ExperimentalHazeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BalanceCard(
    modifier: Modifier = Modifier,
    totalBalance: BigDecimal,
    monthlyChange: BigDecimal = BigDecimal.ZERO,
    monthlyChangePercent: Int = 0,
    spentThisMonth: BigDecimal = BigDecimal.ZERO,
    currency: String,
    abbreviatedName: String,
    userName: String,
    balanceHistory: List<BalancePoint> = emptyList(),
    thisMonthValue: String = "",
    thisYearValue: String = "",
    dateRangeLabel: String = "",
    availableCurrenciesCount: Int = 0,
    onCurrencyClick: () -> Unit = {},
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptic = com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback()
    val containerColor = MaterialTheme.colorScheme.surfaceContainer

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "chevron_rotation"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .springPress(targetScale = 0.98f)
                .animateContentSize(
                    MaterialTheme.motionScheme.fastSpatialSpec()
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
                )
                .clickable {
                    haptic.click()
                    isExpanded = !isExpanded
                },
            shape = VittifyShapes.hero,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = VittifySurface.platterAlpha),
            border = VittifySurface.platterBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VittifySpacing.scaledComfortable, vertical = VittifySpacing.scaled(18.dp))
            ) {
                // Top Header Row: Net Worth Label + User Context & Currency Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = stringResource(R.string.net_worth_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (userName.isNotBlank()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Currency Selector Pill (Rule 15: 50dp pill)
                    if (availableCurrenciesCount > 1) {
                        Surface(
                            onClick = onCurrencyClick,
                            modifier = Modifier.springPress(targetScale = 0.93f),
                            shape = VittifyShapes.pill,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currency,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hero Balance (GEMINI.md Rule 13: Currency symbol smaller, major amount bold headline, paise quieter)
                HeroBalanceFormatted(
                    amount = totalBalance,
                    currency = currency
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Spent & Sparkline Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedSpent = remember(spentThisMonth, currency) {
                        CurrencyFormatter.formatCurrency(spentThisMonth, currency)
                    }

                    // Spent Pill Badge (GEMINI.md Rule 15: 50dp pill, calm tonal surface)
                    Surface(
                        shape = VittifyShapes.pill,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.spent_this_month_format,
                                    formattedSpent
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mini Sparkline (Smooth financial preview)
                    if (balanceHistory.isNotEmpty()) {
                        val isPositiveTrend = if (balanceHistory.size >= 2) {
                            balanceHistory.last().balance >= balanceHistory.first().balance
                        } else monthlyChange >= BigDecimal.ZERO
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(38.dp)
                        ) {
                            BalanceSparkline(
                                data = balanceHistory.sortedBy { it.timestamp }.map { it.balance },
                                lineColor = if (isPositiveTrend) success_dark else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Expandable Section for Detailed History & Breakdown
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
                    exit = fadeOut() + shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = VittifySurface.dividerAlpha))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Chart
                        if (balanceHistory.isNotEmpty()) {
                            BalanceChart(
                                primaryCurrency = currency,
                                balanceHistory = balanceHistory,
                                backgroundColor = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                height = 160
                            )
                            if (dateRangeLabel.isNotBlank()) {
                                Text(
                                    text = dateRangeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = VittifySurface.dividerAlpha))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Summary Breakdown Items
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SummaryItem(
                                label = stringResource(R.string.this_month_lbl),
                                value = thisMonthValue
                            )
                            VerticalDivider(
                                modifier = Modifier.height(28.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = VittifySurface.dividerAlpha)
                            )
                            SummaryItem(
                                label = stringResource(R.string.this_year_lbl),
                                value = thisYearValue
                            )
                            VerticalDivider(
                                modifier = Modifier.height(28.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = VittifySurface.dividerAlpha)
                            )
                            SummaryItem(
                                label = stringResource(R.string.balance_label),
                                value = CurrencyFormatter.formatCurrency(totalBalance, currency)
                            )
                        }
                    }
                }
            }
        }

        // Tactile Expand/Collapse Chevron Indicator
        Icon(
            imageVector = Iconax.LongArrow,
            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .height(20.dp)
                .width(32.dp)
                .rotate(rotation)
        )
    }
}

/**
 * Formats Hero Balance strictly following GEMINI.md Rule 13:
 * Currency symbol: smaller and lighter
 * Major amount: large and bold
 * Decimals/paise: smaller and quieter
 */
@Composable
private fun HeroBalanceFormatted(
    amount: BigDecimal,
    currency: String
) {
    val formatted = remember(amount, currency) {
        CurrencyFormatter.formatCurrency(amount, currency)
    }

    val (symbol, major, decimal) = remember(formatted) {
        // Extract currency symbol if leading
        var sym = ""
        var rest = formatted
        for (char in formatted) {
            if (!char.isDigit() && char != ',' && char != '.' && char != '-' && char != '+') {
                sym += char
            } else {
                break
            }
        }
        rest = formatted.removePrefix(sym).trim()

        if (rest.contains(".")) {
            val parts = rest.split(".", limit = 2)
            Triple(sym.ifBlank { "" }, parts[0], ".${parts[1]}")
        } else {
            Triple(sym.ifBlank { "" }, rest, "")
        }
    }

    AnimatedContent(
        targetState = Triple(symbol, major, decimal),
        transitionSpec = {
            (slideInVertically(
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f)
            ) { height -> height / 2 } + fadeIn(
                animationSpec = spring(stiffness = 400f)
            )) togetherWith (slideOutVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f)
            ) { height -> -height / 2 } + fadeOut(
                animationSpec = spring(stiffness = 450f)
            )) using SizeTransform(clip = false)
        },
        label = "hero_balance_rolling"
    ) { (sym, maj, dec) ->
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            if (sym.isNotBlank()) {
                Text(
                    text = sym,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
                )
            }
            Text(
                text = maj,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (dec.isNotBlank()) {
                Text(
                    text = dec,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 400f)
                ) { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "summary_item_rolling"
        ) { targetVal ->
            Text(
                text = targetVal,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun BalanceSparkline(
    data: List<BigDecimal>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    val (min, _, range) = remember(data) {
        val maxVal = data.maxOf { it }.toFloat()
        val minVal = data.minOf { it }.toFloat()
        val r = (maxVal - minVal).takeIf { it > 0 } ?: 1f
        Triple(minVal, maxVal, r)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val path = Path()

        data.forEachIndexed { index, value ->
            val x = index.toFloat() / (data.size - 1) * width
            val y = height - ((value.toFloat() - min) / range * (height - 8f)) - 4f

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Gradient fill
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
            )
        )
    }
}
