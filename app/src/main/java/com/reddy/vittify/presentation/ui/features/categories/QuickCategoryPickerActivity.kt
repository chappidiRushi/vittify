package com.reddy.vittify.presentation.ui.features.categories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.database.entity.CategoryType
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.presentation.ui.components.CategorySelectionSheet
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.VittifyTheme
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback
import com.reddy.vittify.utils.formatAmount
import dagger.hilt.android.AndroidEntryPoint

/**
 * Transparent overlay activity for fast, real-time category searching
 * triggered from the transaction notification.
 */
@AndroidEntryPoint
class QuickCategoryPickerActivity : ComponentActivity() {

    private val viewModel: QuickCategoryPickerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)

        setContent {
            VittifyTheme {
                QuickCategoryPickerOverlayScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun QuickCategoryPickerOverlayScreen(
    viewModel: QuickCategoryPickerViewModel,
    onDismiss: () -> Unit
) {
    val transaction by viewModel.transaction.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val subcategoriesMap by viewModel.subcategoriesMap.collectAsState()
    val recentSubcategories by viewModel.recentSubcategories.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()

    val haptic = rememberAppHapticFeedback()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            onDismiss()
        }
    }

    val relevantCategories = remember(categories, transaction) {
        val tx = transaction
        if (tx != null) {
            val targetType = when (tx.transactionType) {
                TransactionType.INCOME -> CategoryType.INCOME
                TransactionType.INVESTMENT -> CategoryType.INVESTMENT
                TransactionType.CREDIT -> CategoryType.CREDIT
                else -> CategoryType.EXPENSE
            }
            val filtered = categories.filter { it.categoryType == targetType }
            if (filtered.isNotEmpty()) filtered else categories
        } else {
            categories
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Intercept clicks inside the surface
                )
                .imePadding(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = VittifySurface.surfaceContainerColor(),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = VittifyShapes.pill
                            )
                    )
                }

                // Transaction summary header
                if (transaction != null) {
                    val tx = transaction!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val emoji = when (tx.transactionType) {
                                    TransactionType.INCOME -> "💰"
                                    TransactionType.INVESTMENT -> "📈"
                                    TransactionType.CREDIT -> "💳"
                                    TransactionType.TRANSFER -> "🔄"
                                    else -> "💸"
                                }
                                Text(
                                    text = "$emoji ${tx.formatAmount()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (tx.merchantName.isNotBlank()) {
                                    Text(
                                        text = "• ${tx.merchantName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "Current: ${tx.category}${tx.subcategory?.let { " • $it" } ?: ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                // Live search and dynamically updating suggestions sheet
                CategorySelectionSheet(
                    categories = relevantCategories,
                    subcategoriesMap = subcategoriesMap,
                    recentSubcategories = recentSubcategories,
                    focusRequester = focusRequester,
                    onSelectionComplete = { category, subcategory ->
                        haptic.click()
                        viewModel.selectCategory(category, subcategory)
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

