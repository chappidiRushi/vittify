package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.R
import androidx.core.graphics.toColorInt
import com.reddy.vittify.presentation.effects.BlurredAnimatedVisibility
import com.reddy.vittify.utils.IconResolutionUtils

@Composable
fun CategoryChip(
    category: CategoryEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val iconResId = if (category.iconName.isNotEmpty()) {
            IconResolutionUtils.nameToResId(context, category.iconName)
                .takeIf { it != 0 } ?: R.drawable.type_food_dining
        } else {
            com.reddy.vittify.utils.IconResolutionUtils.getSafeResId(
                context, 
                category.iconResId, 
                R.drawable.type_food_dining
            )
        }


        if (onClick != null) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(44.dp).
                background(
                    color = parseColor(category.color),
                    shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.input
                )
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .padding(4.dp)
                        .animatedIconEntrance()
                        .animatedIconClick(),
                    tint = Color.Unspecified
                )
            }
        }

        
        Spacer(modifier = Modifier.width(16.dp))
        Spacer(modifier = Modifier.width(com.reddy.vittify.presentation.ui.theme.Spacing.md))
        
        // Category name
        if (showText) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BlurredAnimatedVisibility(visible = category.description.isNotEmpty()) {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

        }
    }
}

/**
 * Helper function to parse color string to Compose Color.
 * Handles hex colors like "#FF0000" or "FF0000".
 */
private fun parseColor(colorString: String): Color {
    return try {
        val cleanColor = if (colorString.startsWith("#")) colorString else "#$colorString"
        Color(cleanColor.toColorInt())
    } catch (e: Exception) {
        // Fallback to grey if color parsing fails
        Color(0xFF757575)
    }
}