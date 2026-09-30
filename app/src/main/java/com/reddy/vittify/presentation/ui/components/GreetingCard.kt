package com.reddy.vittify.presentation.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.icons.Edit2
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.Notification
import com.reddy.vittify.presentation.ui.icons.NotificationOutline
import androidx.compose.material.icons.rounded.MoreVert

@Composable
fun GreetingCard(
    modifier: Modifier = Modifier,
    userName: String,
    profileImageUri: Uri? = null,
    profileBackgroundColor: Color = Color.Transparent,
    unreadUpdatesCount: Int = 0,
    showAvatar: Boolean = true,
    showMoreOption: Boolean = false,
    coupleViewModeContent: (@Composable () -> Unit)? = null,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onUpdatesClick: () -> Unit = {},
) {
    val haptic = com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (showAvatar) 16.dp else 0.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAvatar) {
            ProfileAvatar(
                modifier = Modifier.size(50.dp),
                profileImageUri = profileImageUri,
                profileBackgroundColor = profileBackgroundColor,
                onClick = onProfileClick
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        // User Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            val greetingRes = remember {
                val hour = LocalTime.now().hour
                when (hour) {
                    in 5..11 -> R.string.greeting_morning
                    in 12..16 -> R.string.greeting_afternoon
                    in 17..21 -> R.string.greeting_evening
                    else -> R.string.greeting_night
                }
            }

            val locale = java.util.Locale.getDefault()
            val monthStatus = remember(locale) {
                val now = LocalDate.now()
                val lastDay = now.withDayOfMonth(now.lengthOfMonth())
                val daysLeft = ChronoUnit.DAYS.between(now, lastDay)
                val monthName = now.month.getDisplayName(java.time.format.TextStyle.FULL, locale)
                
                when {
                    daysLeft == 0L -> R.string.month_status_last_day to listOf(monthName)
                    daysLeft <= 7 -> R.plurals.month_status_days_left_format to listOf(daysLeft.toInt(), monthName)
                    else -> null
                }
            }

            val subtitleText = when {
                monthStatus != null -> {
                    val (resId, args) = monthStatus
                    if (resId == R.string.month_status_last_day) {
                        stringResource(resId, *args.toTypedArray())
                    } else {
                        pluralStringResource(resId, args[0] as Int, *args.toTypedArray())
                    }
                }
                else -> stringResource(greetingRes)
            }

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            /* Priority Logic:
             1. If unread updates > 0 and (it's not the last day of month OR 50% chance)
             2. If monthStatus is available
             3. Greeting
             */

            /*
            * Planning to add a OTA update sheet and changelog
            * therefore commenting out
            */
//            val showUpdates = unreadUpdatesCount > 0 && (monthStatus == null || Math.random() > 0.5)
//
//            if (showUpdates) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.clickable(onClick = onUpdatesClick)
//                ) {
//                    Text(
//                        text = "$unreadUpdatesCount+ unread updates",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Color(0xFF4285F4)
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Icon(
//                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
//                        contentDescription = null,
//                        tint = Color(0xFF4285F4),
//                        modifier = Modifier.size(16.dp)
//                    )
//                }
//            } else {
//                Text(
//                    text = monthStatus ?: greeting,
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
//                )
//            }
        }

        if (coupleViewModeContent != null) {
            coupleViewModeContent()
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Iconax.NotificationOutline,
                contentDescription = stringResource(R.string.notification),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(28.dp)
                    .clickable {
                        haptic.click()
                        onNotificationClick()
                    }
            )
            if (showMoreOption) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            haptic.click()
                            onMoreClick()
                        }
                )
            }
        }
    }
}
