package com.reddy.vittify.presentation.ui.features.onboarding.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface

/**
 * Step 2: Unified Permissions Hub.
 * Manages Messages, Notifications, Nearby Devices, and Camera permissions with interactive individual
 * grant actions and animated granted / rejected responses.
 */
@Composable
fun PermissionsHubStep(
    smsGranted: Boolean,
    notificationGranted: Boolean,
    nearbyGranted: Boolean,
    cameraGranted: Boolean,
    smsDenied: Boolean = false,
    notificationDenied: Boolean = false,
    nearbyDenied: Boolean = false,
    cameraDenied: Boolean = false,
    onRequestSms: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestNearby: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestAllPermissions: () -> Unit = {},
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.permissions_hub_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.permissions_hub_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Permissions Platter
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.surfaceContainerLowColor()
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 1. Messages / SMS
                PermissionItemRow(
                    icon = Icons.Rounded.Message,
                    title = stringResource(R.string.perm_messages_title),
                    description = stringResource(R.string.perm_messages_desc),
                    isGranted = smsGranted,
                    isDenied = smsDenied,
                    onGrantClick = onRequestSms
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                )

                // 2. Notifications
                PermissionItemRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.perm_notifications_title),
                    description = stringResource(R.string.perm_notifications_desc),
                    isGranted = notificationGranted,
                    isDenied = notificationDenied,
                    onGrantClick = onRequestNotification
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                )

                // 3. Nearby Devices & BLE
                PermissionItemRow(
                    icon = Icons.Rounded.BluetoothSearching,
                    title = stringResource(R.string.perm_nearby_title),
                    description = stringResource(R.string.perm_nearby_desc),
                    isGranted = nearbyGranted,
                    isDenied = nearbyDenied,
                    onGrantClick = onRequestNearby
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                )

                // 4. Camera
                PermissionItemRow(
                    icon = Icons.Rounded.CameraAlt,
                    title = stringResource(R.string.perm_camera_title),
                    description = stringResource(R.string.perm_camera_desc),
                    isGranted = cameraGranted,
                    isDenied = cameraDenied,
                    onGrantClick = onRequestCamera
                )
            }
        }

        // Privacy reassurance badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.permissions_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        val allGranted = smsGranted && notificationGranted && nearbyGranted && cameraGranted

        // Action Buttons: Grant All Permissions / Continue
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (!allGranted) {
                TactilePrimaryButton(
                    text = stringResource(R.string.grant_all_permissions),
                    onClick = onRequestAllPermissions,
                    enabled = true
                )
                TactileTonalButton(
                    text = stringResource(R.string.continue_action),
                    onClick = onContinue,
                    enabled = true
                )
            } else {
                TactilePrimaryButton(
                    text = stringResource(R.string.continue_action),
                    onClick = onContinue,
                    enabled = true
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))
    }
}

private enum class PermissionRowStatus {
    GRANTED,
    REJECTED,
    NOT_DETERMINED
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isDenied: Boolean,
    onGrantClick: () -> Unit
) {
    val status = when {
        isGranted -> PermissionRowStatus.GRANTED
        isDenied -> PermissionRowStatus.REJECTED
        else -> PermissionRowStatus.NOT_DETERMINED
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = when (status) {
                PermissionRowStatus.GRANTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                PermissionRowStatus.REJECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                PermissionRowStatus.NOT_DETERMINED -> VittifySurface.surfaceContainerHighColor()
            },
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when (status) {
                        PermissionRowStatus.GRANTED -> MaterialTheme.colorScheme.primary
                        PermissionRowStatus.REJECTED -> MaterialTheme.colorScheme.error
                        PermissionRowStatus.NOT_DETERMINED -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        AnimatedContent(
            targetState = status,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMedium))
            },
            label = "permBadgeAnim"
        ) { targetStatus ->
            when (targetStatus) {
                PermissionRowStatus.GRANTED -> {
                    Surface(
                        shape = VittifyShapes.pill,
                        color = Color(0xFF00C853).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.perm_status_granted),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00C853)
                            )
                        }
                    }
                }
                PermissionRowStatus.REJECTED -> {
                    Button(
                        onClick = onGrantClick,
                        shape = VittifyShapes.pill,
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.perm_status_rejected),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                PermissionRowStatus.NOT_DETERMINED -> {
                    Button(
                        onClick = onGrantClick,
                        shape = VittifyShapes.pill,
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.perm_status_grant),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
