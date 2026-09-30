package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.VittifySpacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalHazeApi::class)
@Composable
fun HeatmapWidget(
    modifier: Modifier = Modifier,
    data: Map<LocalDate, Int>,
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    val weeksToShow = 26 // Show last 6 months
    val today = LocalDate.now()
    val endDate = today
    // Start from the Monday of N weeks ago
    val startDate = endDate.minusWeeks((weeksToShow - 1).toLong()).with(DayOfWeek.MONDAY)
    
    val monthLabels = remember(startDate, endDate) {
        val allMonthStarts = mutableListOf<Pair<Int, String>>()
        var current = startDate
        var lastMonth = -1
        var weekIndex = 0
        
        while (current <= endDate) {
            if (current.monthValue != lastMonth) {
                val formatter = DateTimeFormatter.ofPattern("MMM")
                allMonthStarts.add(weekIndex to current.format(formatter))
                lastMonth = current.monthValue
            }
            current = current.plusWeeks(1)
            weekIndex++
        }

        val filteredLabels = mutableListOf<Pair<Int, String>>()
        for (i in allMonthStarts.indices) {
            val (week, label) = allMonthStarts[i]
            
            // case for the first label: skip it if it's too close to the second one
            if (i == 0 && allMonthStarts.size > 1) {
                val nextWeek = allMonthStarts[1].first
                if (nextWeek - week < 4) continue
            }
            
            //ensure at least 4 weeks between labels
            if (filteredLabels.isEmpty()) {
                filteredLabels.add(week to label)
            } else {
                val lastAddedWeek = filteredLabels.last().first
                if (week - lastAddedWeek >= 4) {
                    filteredLabels.add(week to label)
                }
            }
        }
        filteredLabels
    }

    val scrollState = rememberScrollState()
    
    // Scroll to end (latest data) on initial load
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VittifySpacing.scaledStandard)
            .clip(VittifyShapes.platter)
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
        shape = VittifyShapes.platter,
        color = VittifySurface.platterContainerColor(),
        border = VittifySurface.platterBorder(),
    ) {
        Column(
            modifier = Modifier
                .padding(VittifySpacing.scaledStandard)
                .horizontalScroll(scrollState)
        ) {
            // Heatmap Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // iterate through weeks
                for (w in 0 until weeksToShow) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Iterate through days of the week (Mon to Sun)
                        for (d in 0 until 7) {
                            val date = startDate.plusWeeks(w.toLong()).plusDays(d.toLong())
                            val count = data[date] ?: 0
                            
                            val primary = MaterialTheme.colorScheme.primary
                            val color = when {
                                date > today -> MaterialTheme.colorScheme.surfaceContainerHigh
                                count == 0 -> MaterialTheme.colorScheme.surfaceContainerHigh
                                count == 1 -> primary.copy(alpha = 0.25f)
                                count < 3 -> primary.copy(alpha = 0.5f)
                                count < 5 -> primary.copy(alpha = 0.75f)
                                else -> primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(VittifyShapes.scaled(4.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }

            // Labels (Months)
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                monthLabels.forEach { (weekIndex, label) ->
                    // horizontal offset based on weekIndex
                    val xOffset = (weekIndex * 18).dp
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.offset(x = xOffset)
                    )
                }
            }
        }
    }
}
