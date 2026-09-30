package com.reddy.vittify.presentation.ui.features.budgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.data.database.entity.BudgetTrackType
import com.reddy.vittify.data.database.entity.BudgetType
import com.reddy.vittify.presentation.ui.icons.Box2
import com.reddy.vittify.presentation.ui.icons.Folder2
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.ReceiptItem
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R

@Composable
fun BudgetTypeSelectionSheet(
    onTypeSelected: (BudgetType) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .padding(bottom = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.select_budget_type),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = Spacing.md)
        )

        SelectionCard(
            title = stringResource(R.string.savings_budget),
            description = stringResource(R.string.savings_budget_desc),
            icon = Icons.Rounded.Savings,
            onClick = { onTypeSelected(BudgetType.SAVINGS) }
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        SelectionCard(
            title = stringResource(R.string.expense_budget),
            description = stringResource(R.string.expense_budget_desc),
            icon = Iconax.ReceiptItem,
            onClick = { onTypeSelected(BudgetType.EXPENSE) }
        )
    }
}

@Composable
fun BudgetTrackTypeSelectionSheet(
    onTrackTypeSelected: (BudgetTrackType) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .padding(bottom = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.select_tracking_mode),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = Spacing.md)
        )

        SelectionCard(
            title = stringResource(R.string.added_only),
            description = stringResource(R.string.added_only_desc),
            example = stringResource(R.string.added_only_example),
            icon = Iconax.Folder2,
            onClick = { onTrackTypeSelected(BudgetTrackType.ADDED_ONLY) }
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        SelectionCard(
            title = stringResource(R.string.all_transactions),
            description = stringResource(R.string.all_transactions_desc),
            example = stringResource(R.string.all_transactions_example),
            icon = Iconax.Box2,
            onClick = { onTrackTypeSelected(BudgetTrackType.ALL_TRANSACTIONS) }
        )
    }
}

@Composable
private fun SelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    example: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(VittifyShapes.input)
            .clickable(onClick = onClick),
        shape = VittifyShapes.input,
        colors = CardDefaults.cardColors(
            containerColor = VittifySurface.surfaceContainerLowColor()
        ),
        border = VittifySurface.platterBorder()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(VittifyShapes.scaled(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (example != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = example,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
