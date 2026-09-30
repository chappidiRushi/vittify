package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.R
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.reddy.vittify.presentation.ui.icons.Bag
import com.reddy.vittify.presentation.ui.icons.Copy
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.presentation.effects.BlurredAnimatedVisibility
import com.reddy.vittify.presentation.ui.icons.Card
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.LocalVittifyTokens
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.VittifySpacing
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.credit_dark
import com.reddy.vittify.presentation.ui.theme.credit_light
import com.reddy.vittify.presentation.ui.theme.expense_dark
import com.reddy.vittify.presentation.ui.theme.expense_light
import com.reddy.vittify.presentation.ui.theme.income_dark
import com.reddy.vittify.presentation.ui.theme.income_light
import com.reddy.vittify.presentation.ui.theme.investment_dark
import com.reddy.vittify.presentation.ui.theme.investment_light
import com.reddy.vittify.presentation.ui.theme.transfer_dark
import com.reddy.vittify.presentation.ui.theme.transfer_light
import com.reddy.vittify.utils.CurrencyFormatter
import com.reddy.vittify.utils.formatAmount
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import androidx.core.graphics.toColorInt
import com.reddy.vittify.presentation.ui.icons.Calendar
import com.reddy.vittify.presentation.ui.icons.DocumentText2
import com.reddy.vittify.presentation.ui.icons.Paperclip2

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.BlurredEdgeTreatment
import com.reddy.vittify.presentation.ui.components.animatedIconClick
import com.reddy.vittify.presentation.ui.components.animatedIconEntrance
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.TransactionItem(
    modifier: Modifier = Modifier,
    transaction: TransactionEntity? = null,
    merchantName: String? = null,
    amount: BigDecimal? = null,
    transactionType: TransactionType? = null,
    categoryEntity: CategoryEntity? = null,
    subcategoryEntity: SubcategoryEntity? = null,
    accountIconResId: Int = 0,
    accountIconName: String? = null,
    accountColorHex: String? = null,
    showDate: Boolean = true,
    useCardStyle: Boolean = false,
    shape: CornerBasedShape = listSingleItemShape,
    onClick: () -> Unit = {},
    subtitleOverride: String? = null,
    amountOverride: String? = null,
    amountColorOverride: Color? = null,
    balanceAfter: BigDecimal? = null,
    balanceCurrency: String? = null,
    animatedContentScope: AnimatedVisibilityScope? = null,
    sharedElementKey: String? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionToggle: () -> Unit = {},
    onLongClick: () -> Unit = {},
    convertedAmount: BigDecimal? = null,
    mainCurrency: String? = null,
    currentAccountContext: String? = null,
    currentBankNameContext: String? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    blurEffects: Boolean = false,
    isSplit: Boolean = false,
    isPartner: Boolean = false,
    partnerBadgeLabel: String? = null,
    useSeamlessPlatterStyle: Boolean = false
) {
    val finalMerchantName = merchantName ?: transaction?.merchantName ?: ""
    val finalAmount = amount ?: transaction?.amount ?: BigDecimal.ZERO
    val finalType = transactionType ?: transaction?.transactionType ?: TransactionType.EXPENSE
    val isRecurring = transaction?.isRecurring ?: false
    val isDark = isSystemInDarkTheme()
    var showMenu by remember { mutableStateOf(false) }

    val effectiveSign = remember(transaction, currentAccountContext) {
        if (transaction?.transactionType == TransactionType.TRANSFER && currentAccountContext != null) {
            val isSender = transaction.fromAccount == currentAccountContext
            val isReceiver = !isSender && transaction.toAccount == currentAccountContext
            
            if (isSender) "-" else if (isReceiver) "+" else null
        } else null
    }

    val amountColor = amountColorOverride ?: remember(finalType, isRecurring, isDark, effectiveSign) {
        if (effectiveSign == "-") {
            if (!isDark) expense_light else expense_dark
        } else if (effectiveSign == "+") {
            if (!isDark) income_light else income_dark
        } else {
            when (finalType) {
                TransactionType.INCOME -> if (!isDark) income_light else income_dark
                TransactionType.EXPENSE -> if (!isDark) expense_light else expense_dark
                TransactionType.CREDIT -> if (!isDark) credit_light else credit_dark
                TransactionType.TRANSFER -> if (!isDark) transfer_light else transfer_dark
                TransactionType.INVESTMENT -> if (!isDark) investment_light else investment_dark
                TransactionType.BALANCE_UPDATE -> if (!isDark) transfer_light else transfer_dark
            }
        }
    }

    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val defaultSubtitle = remember(transaction?.dateTime) { 
        transaction?.dateTime?.format(dateTimeFormatter) ?: "" 
    }
    val amountText = remember(transaction, amountOverride, finalAmount, effectiveSign) {
        amountOverride ?: transaction?.let { 
             val formatted = it.formatAmount()
             if (effectiveSign != null) "$effectiveSign $formatted" else formatted
        } ?: finalAmount.toString()
    }

    val dateTagColor = remember(transaction?.dateTime,) {
        val colors = listOf(income_dark, expense_dark, credit_dark, transfer_dark, investment_dark)

        val dateHash = transaction?.dateTime?.toLocalDate()?.hashCode() ?: 0
        val index = Math.abs(dateHash) % colors.size
        colors[index]
    }

    val itemModifier = modifier.then(
        if (animatedContentScope != null && sharedElementKey != null) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = sharedElementKey),
                animatedVisibilityScope = animatedContentScope,
                boundsTransform = { _, _ ->
                    spring(
                        stiffness =  Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Fit, Alignment.Center),
                clipInOverlayDuringTransition = OverlayClip(shape),
                renderInOverlayDuringTransition = false
            )
                .skipToLookaheadSize()
        } else Modifier
    )

    val leadingContent: @Composable () -> Unit = {
        if (isSelectionMode) {
            VittifyCheckbox(
                checked = isSelected,
                onCheckedChange = { onSelectionToggle() },
                modifier = Modifier.size(40.dp)
            )
        } else {
            BrandIcon(
                merchantName = finalMerchantName,
                size = 40.dp,
                showBackground = true,
                categoryEntity = categoryEntity,
                subcategoryEntity = subcategoryEntity,
                category = transaction?.category,
                subcategory = transaction?.subcategory,
                accountIconResId = accountIconResId,
                accountIconName = accountIconName,
                accountColorHex = accountColorHex,
                isSplit = isSplit,
                modifier = if (animatedContentScope != null && transaction != null) {
                    Modifier.sharedElement(
                        rememberSharedContentState(key = "brand_icon_${transaction.id}"),
                        animatedVisibilityScope = animatedContentScope
                    )
                } else Modifier
            )
        }
    }

    // Build subtitle parts
    val recurringStr = stringResource(R.string.recurring)
    val balanceAfterStr = balanceAfter?.let { balance ->
        stringResource(R.string.balance_after_format, CurrencyFormatter.formatCurrency(balance, balanceCurrency ?: "INR"))
    }
    val (subtitleParts, subtitleFinal) = remember(
        subtitleOverride,
        defaultSubtitle,
        isRecurring,
        recurringStr,
        balanceAfterStr
    ) {
        val parts = buildList {
            if (subtitleOverride != null) {
                add(subtitleOverride)
            } else {
                if (defaultSubtitle.isNotEmpty()) {
                    add(defaultSubtitle)
                }
                if (isRecurring) add(recurringStr)

                balanceAfterStr?.let { add(it) }
            }
        }
        parts to parts.joinToString(" • ")
    }

    if (useCardStyle) {
        ListItemCard(
            title = finalMerchantName,
            subtitle = subtitleFinal,
            amount = amountText,
            amountColor = amountColor,
            onClick = onClick,
            leadingContent = leadingContent,
            modifier = itemModifier
        )
    } else {
        ListItem(
            headline = {
                Text(
                    text = finalMerchantName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supporting = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    var needsSeparator = false
                    
                    @Composable
                    fun TagSeparator() {
                        if (needsSeparator) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                            )
                        }
                    }

                    // Date Tag
                    if (subtitleOverride == null && defaultSubtitle.isNotEmpty()) {
                        TagSeparator()
                        SubtitleTag(
                            icon = {
                                Icon(
                                    imageVector = Iconax.Calendar,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.85f)
                                )
                            },
                            text = defaultSubtitle,
                            color = dateTagColor,
                        )
                        needsSeparator = true
                    }

                    // Partner Tag
                    if (isPartner) {
                        TagSeparator()
                        SubtitleTag(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            text = partnerBadgeLabel ?: "Partner",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        needsSeparator = true
                    }

                    // Category Tag
                    if (isSplit) {
                        TagSeparator()
                        SubtitleTag(
                            text = stringResource(R.string.split_transaction),
                            color = MaterialTheme.colorScheme.primary
                        )
                        needsSeparator = true
                    } else {
                        categoryEntity?.let { category ->
                            TagSeparator()
                            SubtitleTag(
                                text = category.name,
                                color = try {
                                    Color(category.color.toColorInt())
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            needsSeparator = true
                        }
                    }

                    if (subtitleOverride != null) {
                        TagSeparator()
                        Text(
                            text = subtitleOverride,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        needsSeparator = true
                    } else {
                        // Recurring Tag
                        if (isRecurring) {
                            TagSeparator()
                            SubtitleTag(
                                text = stringResource(R.string.recurring),
                                color = Color(0xFF5B54D6)
                            )
                            needsSeparator = true
                        }


                        // Balance Tag
                        balanceAfter?.let { balance ->
                            TagSeparator()
                            SubtitleTag(
                                text = stringResource(R.string.balance_after_format, CurrencyFormatter.formatCurrency(balance, balanceCurrency ?: "INR")),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            needsSeparator = true
                        }

                        // Description Indicator
                        if (transaction?.description?.isNotBlank() == true) {
                            TagSeparator()
                            Icon(
                                imageVector = Iconax.DocumentText2,
                                contentDescription = stringResource(R.string.description_indicator_desc),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                            )
                            needsSeparator = true
                        }

                        // Attachments Indicator
                        if (transaction?.attachments?.isNotBlank() == true) {
                            TagSeparator()
                            Icon(
                                imageVector = Iconax.Paperclip2,
                                contentDescription = stringResource(R.string.attachments_indicator_desc),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                            )
                            needsSeparator = true
                        }
                    }
                }
            },
            leading = leadingContent,
            trailing = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        if (subtitleOverride == null) {
                            when (finalType) {
                                TransactionType.CREDIT -> Icon(
                                    Iconax.Card,
                                    contentDescription = stringResource(R.string.type_credit_card),
                                    modifier = Modifier.size(Dimensions.Icon.small).animatedIconEntrance(),
                                    tint = if (!isSystemInDarkTheme()) credit_light else credit_dark
                                )

                                TransactionType.TRANSFER -> Icon(
                                    Icons.Rounded.SwapHoriz,
                                    contentDescription = stringResource(R.string.type_transfer),
                                    modifier = Modifier.size(Dimensions.Icon.small).animatedIconEntrance(),
                                    tint = if (!isSystemInDarkTheme()) transfer_light else transfer_dark
                                )

                                TransactionType.INVESTMENT -> Icon(
                                    Icons.AutoMirrored.Filled.ShowChart,
                                    contentDescription = stringResource(R.string.type_investment),
                                    modifier = Modifier.size(Dimensions.Icon.small).animatedIconEntrance(),
                                    tint = if (!isSystemInDarkTheme()) investment_light else investment_dark
                                )

                                TransactionType.INCOME -> Icon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = stringResource(R.string.type_income),
                                    modifier = Modifier.size(Dimensions.Icon.small).animatedIconEntrance(),
                                    tint = if (!isSystemInDarkTheme()) income_light else income_dark
                                )

                                TransactionType.EXPENSE -> Icon(
                                    Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = stringResource(R.string.type_expense),
                                    modifier = Modifier.size(Dimensions.Icon.small).animatedIconEntrance(),
                                    tint = if (!isSystemInDarkTheme()) expense_light else expense_dark
                                )

                                TransactionType.BALANCE_UPDATE -> Icon(
                                    Icons.Rounded.SwapHoriz,
                                    contentDescription = stringResource(R.string.type_balance_update),
                                    modifier = Modifier.size(Dimensions.Icon.small).animatedIconEntrance(),
                                    tint = if (!isSystemInDarkTheme()) transfer_light else transfer_dark
                                )
                            }
                        }
                        Text(
                            text = amountText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = amountColor
                        )
                    }

                    if (convertedAmount != null && mainCurrency != null && transaction?.currency != mainCurrency) {
                        Text(
                            text = "≈ ${CurrencyFormatter.formatCurrency(convertedAmount, mainCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Three-dot menu
                    if (!isSelectionMode && (onDuplicate != null || onDelete != null || onReport != null)) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Rounded.MoreHoriz,
                                    contentDescription = stringResource(R.string.more_options),
                                    modifier = Modifier.size(16.dp).animatedIconClick(),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            val dropContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier
                                    .clip(VittifyShapes.input)
                                    .then(
                                        if (blurEffects && hazeState != null) Modifier.hazeEffect(
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
                                    )
                                    .then(
                                        VittifySurface.platterBorder()?.let { Modifier.border(it, VittifyShapes.input) } ?: Modifier
                                    ),
                                containerColor = dropContainerColor.copy(
                                    alpha = if (blurEffects && hazeState != null) 0.7f else LocalVittifyTokens.current.surfaceOpacity
                                ),
                                shape = VittifyShapes.input
                            ) {
                                onReport?.let {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.report_issue)) },
                                        onClick = {
                                            showMenu = false
                                            it()
                                        },
                                        leadingIcon = { Icon(androidx.compose.material.icons.Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    )
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.surface.copy(0.6f)
                                    )
                                }
                                onDuplicate?.let {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.duplicate_transaction)) },
                                        onClick = {
                                            showMenu = false
                                            it()
                                        },
                                        leadingIcon = { Icon(Iconax.Copy, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    )
                                }
                                onDelete?.let {
                                    if (onDuplicate != null) {
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.surface.copy(0.6f)
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_transaction)) },
                                        onClick = {
                                            showMenu = false
                                            it()
                                        },
                                        leadingIcon = { Icon(Iconax.Bag, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            onClick = {
                if (isSelectionMode) {
                    onSelectionToggle()
                } else {
                    onClick()
                }
            },
            onLongClick = onLongClick,
            shape = if (useSeamlessPlatterStyle) null else shape,
            border = if (useSeamlessPlatterStyle) null else VittifySurface.platterBorder(),
            useDefaultBorder = !useSeamlessPlatterStyle,
            listColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = (LocalVittifyTokens.current.surfaceOpacity * 0.95f).coerceIn(0.4f, 1f)
                )
            } else if (useSeamlessPlatterStyle) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = LocalVittifyTokens.current.surfaceOpacity)
            },
            padding = if (useSeamlessPlatterStyle) PaddingValues(horizontal = VittifySpacing.scaledMicro, vertical = 2.dp) else PaddingValues(vertical = 1.5.dp),
            modifier = itemModifier
        )
    }
}


