package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reddy.vittify.utils.IconResolutionUtils
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.presentation.common.icons.CategoryMapping
import com.reddy.vittify.presentation.common.icons.IconProvider
import com.reddy.vittify.presentation.common.icons.IconResource

/**
 * Displays a brand icon with intelligent fallback
 * Fallback priority: Brand Icon > Subcategory Icon > Category Icon
 */
@Composable
fun BrandIcon(
    merchantName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showBackground: Boolean = true,
    categoryEntity: CategoryEntity? = null,
    subcategoryEntity: SubcategoryEntity? = null,
    category: String? = null,
    subcategory: String? = null,
    accountIconResId: Int = 0,
    accountIconName: String? = null,
    accountColorHex: String? = null,
    isSplit: Boolean = false
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val iconResource = remember(merchantName, categoryEntity, subcategoryEntity, category, subcategory, accountIconResId, accountIconName, isSplit) {
        if (isSplit) {
            IconResource.VectorIcon(Icons.Rounded.SwapVert, primaryColor)
        } else {
            IconProvider.getIconForTransaction(
                context = context,
                merchantName = merchantName,
                categoryEntity = categoryEntity,
                subcategoryEntity = subcategoryEntity,
                category = category,
                subcategory = subcategory,
                accountIconResId = accountIconResId,
                accountIconName = accountIconName
            )
        }
    }
    val brandColor = remember(merchantName, categoryEntity, subcategoryEntity, category, subcategory, accountColorHex, isSplit) {
        if (isSplit) {
            Pair(String.format("#%06X", (0xFFFFFF and primaryColor.toArgb())), 0.2f)
        } else {
            IconProvider.getColorForTransaction(
                merchantName = merchantName,
                categoryEntity = categoryEntity,
                subcategoryEntity = subcategoryEntity,
                category = category,
                subcategory = subcategory,
                accountColorHex = accountColorHex
            )
        }
    }
    
    val internalPadding = remember(iconResource, showBackground) {
        // Only pad inward when there is a background circle to inset from.
        // Without a background, padding just shrinks the visible icon.
        if (!showBackground || iconResource is IconResource.DrawableResource) 0.dp else 8.dp
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackground) {
                    Modifier
                        .clip(CircleShape)
                        .background(
                            brandColor?.let { (colorHex, alpha) -> 
                                Color(colorHex.toColorInt()).copy(alpha = alpha)
                            } ?: generateColorFromString(merchantName)
                        )
                } else {
                    Modifier
                }
            )
            .animatedIconEntrance()
            .animatedIconClick()
            .padding(internalPadding),
        contentAlignment = Alignment.Center
    ) {
        when (iconResource) {
            is IconResource.DrawableResource -> {
                // Brand logo
                Image(
                    painter = painterResource(id = iconResource.resId),
                    contentDescription = merchantName,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is IconResource.VectorIcon -> {
                // Category icon fallback
                Icon(
                    imageVector = iconResource.icon,
                    contentDescription = merchantName,
                    tint = if (showBackground) Color.White else iconResource.tint,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is IconResource.TintedResIcon -> {
                Icon(
                    painter = painterResource(id = iconResource.resId),
                    contentDescription = merchantName,
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun CategoryIcon(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color? = Color.Unspecified,
    iconResId: Int = 0,
    iconName: String? = null
) {
    val context = LocalContext.current
    val painter = if (!iconName.isNullOrEmpty()) {
        val resId = IconResolutionUtils.nameToResId(context, iconName)
        if (resId != 0) painterResource(id = resId) else {
            val categoryInfo = CategoryMapping.categories[category]
                ?: CategoryMapping.categories["Miscellaneous"]!!
            painterResource(id = categoryInfo.iconResId)
        }
    } else if (iconResId != 0) {
        val safeResId = IconResolutionUtils.getSafeResId(context, iconResId, 0)
        if (safeResId != 0) {
            painterResource(id = safeResId)
        } else {
            val categoryInfo = CategoryMapping.categories[category]
                ?: CategoryMapping.categories["Miscellaneous"]!!
            painterResource(id = categoryInfo.iconResId)
        }
    } else {
        val categoryInfo = CategoryMapping.categories[category]
            ?: CategoryMapping.categories["Miscellaneous"]!!
        painterResource(id = categoryInfo.iconResId)
    }

    
    Icon(
        painter = painter,
        contentDescription = category,
        tint = tint ?: Color.Unspecified,
        modifier = modifier
            .size(size)
            .animatedIconEntrance()
            .animatedIconClick()
    )
}

/**
 * Utility function to generate consistent colors from strings
 */
private fun generateColorFromString(str: String): Color {
    val colors = listOf(
        Color(0xFF6750A4), // Material Purple
        Color(0xFF0061A4), // Material Blue
        Color(0xFF006D40), // Material Green
        Color(0xFFB3261E), // Material Red
        Color(0xFF9A4521), // Material Orange
        Color(0xFF6D4C41), // Material Brown
        Color(0xFF455A64), // Material Blue Grey
        Color(0xFF5E35B1), // Deep Purple
        Color(0xFF43A047), // Green
        Color(0xFFE53935), // Red
    )
    
    val hash = str.hashCode()
    return colors[Math.abs(hash) % colors.size]
}

/**
 * Extension to convert hex string to Color Int
 */
private fun String.toColorInt(): Int {
    // Remove # if present and parse hex
    val hex = this.removePrefix("#")
    return android.graphics.Color.parseColor("#$hex")
}