package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.presentation.common.icons.IconProvider
import com.reddy.vittify.presentation.ui.icons.Bag
import com.reddy.vittify.presentation.ui.icons.Balance
import com.reddy.vittify.presentation.ui.icons.ReceiptSearch
import com.reddy.vittify.presentation.ui.icons.Edit2
import com.reddy.vittify.presentation.ui.icons.Eye
import com.reddy.vittify.presentation.ui.icons.EyeSlash
import com.reddy.vittify.presentation.ui.icons.HierarchySquare3
import com.reddy.vittify.presentation.ui.icons.History
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.LocalVittifyTokens
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback
import com.reddy.vittify.utils.CurrencyFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountCard(
    account: AccountBalanceEntity,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape? = null,
    isHidden: Boolean = false,
    showMoreOptions: Boolean = true,
    onClick: (() -> Unit)? = null,
    isMain: Boolean = false,
    onUpdateBalance: () -> Unit = {},
    onEditAccount: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onAuditBalance: () -> Unit = {},
    onToggleVisibility: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onSetAsMain: () -> Unit = {},
    onMergeAccount: (() -> Unit)? = null,
    onSettleBill: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    val haptic = com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback()
    val tokens = LocalVittifyTokens.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (onClick != null) {
                        haptic.click()
                        onClick()
                    }
                },
                onLongClick = {
                    haptic.longClick()
                    showBottomSheet = true
                }
            ),
        shape = shape ?: VittifyShapes.platter,
        colors = CardDefaults.cardColors(
            containerColor = if (isHidden) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = (tokens.surfaceOpacity * 0.85f).coerceIn(0.2f, 1f))
            else VittifySurface.platterContainerColor()
        ),
        border = VittifySurface.platterBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val context = LocalContext.current
            val iconResource = remember(account.bankName, account.iconResId, account.iconName) {
                IconProvider.getIconForTransaction(
                    context = context,
                    merchantName = account.bankName,
                    accountIconResId = account.iconResId,
                    accountIconName = account.iconName
                )
            }

            TiledScrollingIconBackground(
                iconResource = iconResource,
                opacity = 0.05f,
                iconSize = 56.dp
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (account.isCreditCard) "Outstanding" else "Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showMoreOptions) {
                        IconButton(
                            onClick = {
                                haptic.click()
                                showBottomSheet = true
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "More options",
                            )
                        }
                    }
                }

                // Balance
                Text(
                    text = CurrencyFormatter.formatCurrency(
                        account.balance,
                        account.currency
                    ),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Bottom Section (Bank Info)
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.90f),
                                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.90f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = account.bankName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (account.isWallet) "wallet"
                                    else "**** **** **** ${account.accountLast4}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                if (isMain) {
                                    Surface(
                                        shape = VittifyShapes.pill,
                                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                                        border = BorderStroke(
                                            1.dp,
                                            Color(0xFFFFD700).copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(Spacing.xs),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(12.dp).animatedIconEntrance()
                                            )
                                        }
                                    }
                                }

                                BrandIcon(
                                    merchantName = account.bankName,
                                    size = 48.dp,
                                    showBackground = true,
                                    accountIconResId = account.iconResId,
                                    accountIconName = account.iconName,
                                    accountColorHex = account.color
                                )
                            }
                        }

                    }
                    // Extra content (e.g., Credit Card stats, Linked Cards)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.90f))
                    ) {
                        content()
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        AccountActionsBottomSheet(
            account = account,
            isHidden = isHidden,
            isMain = isMain,
            onDismiss = { showBottomSheet = false },
            onSettleBill = onSettleBill,
            onUpdateBalance = onUpdateBalance,
            onEditAccount = onEditAccount,
            onMergeAccount = onMergeAccount,
            onViewHistory = onViewHistory,
            onAuditBalance = onAuditBalance,
            onToggleVisibility = onToggleVisibility,
            onSetAsMain = onSetAsMain,
            onDeleteAccount = onDeleteAccount
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountActionsBottomSheet(
    account: AccountBalanceEntity,
    isHidden: Boolean,
    isMain: Boolean,
    onDismiss: () -> Unit,
    onSettleBill: (() -> Unit)?,
    onUpdateBalance: () -> Unit,
    onEditAccount: () -> Unit,
    onMergeAccount: (() -> Unit)?,
    onViewHistory: () -> Unit,
    onAuditBalance: () -> Unit,
    onToggleVisibility: () -> Unit,
    onSetAsMain: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    VittifyModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                BrandIcon(
                    merchantName = account.bankName,
                    size = 44.dp,
                    showBackground = true,
                    accountIconResId = account.iconResId,
                    accountIconName = account.iconName,
                    accountColorHex = account.color
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.bankName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (account.isWallet) "Wallet" else "•••• ${account.accountLast4}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // Actions platter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(VittifyShapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (onSettleBill != null) {
                    AccountActionRow(
                        title = "Settle Bill",
                        icon = Icons.Rounded.SwapHoriz,
                        onClick = {
                            onDismiss()
                            onSettleBill()
                        }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )
                }

                AccountActionRow(
                    title = "Update Balance",
                    icon = Iconax.Balance,
                    onClick = {
                        onDismiss()
                        onUpdateBalance()
                    }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )

                AccountActionRow(
                    title = "Edit Details",
                    icon = Iconax.Edit2,
                    onClick = {
                        onDismiss()
                        onEditAccount()
                    }
                )

                if (onMergeAccount != null) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )
                    AccountActionRow(
                        title = "Merge Account",
                        icon = Iconax.HierarchySquare3,
                        onClick = {
                            onDismiss()
                            onMergeAccount()
                        }
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )

                AccountActionRow(
                    title = "History",
                    icon = Iconax.History,
                    onClick = {
                        onDismiss()
                        onViewHistory()
                    }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )

                AccountActionRow(
                    title = "Audit Balance",
                    icon = Iconax.ReceiptSearch,
                    onClick = {
                        onDismiss()
                        onAuditBalance()
                    }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )

                AccountActionRow(
                    title = if (isHidden) "Show Account" else "Hide Account",
                    icon = if (isHidden) Iconax.Eye else Iconax.EyeSlash,
                    onClick = {
                        onDismiss()
                        onToggleVisibility()
                    }
                )

                if (!isMain) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )
                    AccountActionRow(
                        title = "Set as Main Account",
                        icon = Icons.Rounded.Star,
                        iconTint = Color(0xFFFFD700),
                        onClick = {
                            onDismiss()
                            onSetAsMain()
                        }
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )

                AccountActionRow(
                    title = "Delete Account",
                    icon = Iconax.Bag,
                    isDestructive = true,
                    onClick = {
                        onDismiss()
                        onDeleteAccount()
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountActionRow(
    title: String,
    icon: ImageVector,
    iconTint: Color? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = rememberAppHapticFeedback()
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val resolvedIconTint = iconTint ?: contentColor

    Surface(
        onClick = {
            if (isDestructive) haptic.longClick() else haptic.click()
            onClick()
        },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = resolvedIconTint
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}