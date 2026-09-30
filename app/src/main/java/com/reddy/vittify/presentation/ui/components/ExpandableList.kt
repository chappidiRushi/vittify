package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reddy.vittify.presentation.ui.theme.Spacing

/**
 * Reusable expandable list component
 * Shows limited items initially with "View All" button
 */
@Composable
fun <T> ExpandableList(
    items: List<T>,
    visibleItemCount: Int = 5,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Int, Int, T) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val displayItems = if (isExpanded) items else items.take(visibleItemCount)

    
    Column(modifier = modifier) {
        // Display items
        displayItems.forEachIndexed { index, item ->
            itemContent(index, displayItems.size, item)
        }
        
        // View All / View Less button
        if (items.size > visibleItemCount) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.height(26.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) {
                            "View Less"
                        } else {
                            "View ${items.size - visibleItemCount} more"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        modifier = Modifier.padding(horizontal = Spacing.md)
                    )
                }
            }
        }
    }
}