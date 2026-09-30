package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.RefreshCircle
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.utils.capitalizeFirst
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CustomBillingCycleCard(
    count: Int,
    unit: String,
    endDate: LocalDate?,
    onCountClick: () -> Unit,
    onUnitSelected: (String) -> Unit,
    onEndDateClick: () -> Unit,
    onClearEndDate: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null
) {
    var showUnitMenu by remember { mutableStateOf(false) }
    val resolvedShape = shape ?: VittifyShapes.input

    Surface(
        color = VittifySurface.platterCardColor(),
        shape = resolvedShape,
        border = VittifySurface.platterBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: "Repeat every" and "Clear End Date"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Iconax.RefreshCircle,
                        contentDescription = "repeat",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Repeat every",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }

                if (endDate != null) {
                    IconButton(
                        onClick = onClearEndDate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Set to Forever",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Selection Row: Count, Unit, Dashed Line, End Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.weight(1f)
                ) {
                    // Count Box
                    Surface(
                        onClick = onCountClick,
                        shape = VittifyShapes.small,
                        color = VittifySurface.platterCardColor(),
                        border = VittifySurface.platterBorder(),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                count.toString(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    // Unit Box
                    Box {
                        Surface(
                            onClick = { showUnitMenu = true },
                            shape = VittifyShapes.small,
                            color = VittifySurface.platterCardColor(),
                            border = VittifySurface.platterBorder(),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    unit.capitalizeFirst(),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showUnitMenu,
                            onDismissRequest = { showUnitMenu = false },
                            shape = VittifyShapes.input
                        ) {
                            val units = listOf("day", "week", "month", "year")
                            units.forEachIndexed { index, u ->
                                val isFirstItem = index == 0
                                DropdownMenuItem(
                                    text = {
                                        Text(u.capitalizeFirst())
                                    },
                                    onClick = {
                                        onUnitSelected(u)
                                        showUnitMenu = false
                                    }
                                )
                                if (index < units.size - 1) {
                                    HorizontalDivider(
                                        thickness = 1.5.dp,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }
                    }
                }

                DashedLine(
                    modifier = Modifier.weight(0.5f),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )

                // End Date Selection
                val endDateLabel = endDate?.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                ) ?: "Forever"

                Surface(
                    onClick = onEndDateClick,
                    shape = VittifyShapes.small,
                    color = VittifySurface.platterCardColor(),
                    border = VittifySurface.platterBorder(),
                    modifier = Modifier.height(40.dp).weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Until $endDateLabel",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
