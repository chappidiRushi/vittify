package com.reddy.vittify.presentation.ui.features.onboarding.steps

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.components.ColorPickerContent
import com.reddy.vittify.presentation.ui.features.profile.EditProfileState
import com.reddy.vittify.presentation.ui.features.profile.PresetAvatarSelection
import com.reddy.vittify.presentation.ui.icons.CloseCircle
import com.reddy.vittify.presentation.ui.icons.Edit2
import com.reddy.vittify.presentation.ui.icons.GalleryExport
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface

/**
 * Step 5: Profile Customization.
 * Implements full profile personalization matching EditProfileSheet:
 * Real-time live profile preview card, name input, 3D preset avatars, gallery picker,
 * avatar clear action, and custom background color palette.
 */
@Composable
fun ProfileSetupStep(
    profileState: EditProfileState,
    onNameChange: (String) -> Unit,
    onProfileImageChange: (Uri?) -> Unit,
    onBackgroundColorChange: (Color) -> Unit,
    onSaveProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onProfileImageChange(uri)
    }

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
                text = stringResource(R.string.profile_setup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.profile_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Live Profile Preview Card
        LiveProfilePreviewCard(
            userName = profileState.editedUserName,
            avatarUri = profileState.editedProfileImageUri,
            backgroundColor = profileState.editedProfileBackgroundColor
        )

        // 2. Name Input
        TextField(
            value = profileState.editedUserName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.what_should_we_call_you)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.input,
            leadingIcon = { Icon(Iconax.Edit2, contentDescription = null, modifier = Modifier.size(20.dp)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                unfocusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        // 3. Preset Avatars & Gallery Upload
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.surfaceContainerLowColor()
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.choose_avatar),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PresetAvatarSelection(
                    selectedUri = profileState.editedProfileImageUri,
                    onSelect = onProfileImageChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = VittifyShapes.button,
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(Iconax.GalleryExport, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.gallery), style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { onProfileImageChange(null) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = VittifyShapes.button,
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(Iconax.CloseCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.clear), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 4. Background Color Palette & Picker
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VittifyShapes.hero,
            colors = CardDefaults.cardColors(
                containerColor = VittifySurface.surfaceContainerLowColor()
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = stringResource(R.string.profile_color),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ColorPickerContent(
                    initialColor = profileState.editedProfileBackgroundColor.toArgb(),
                    onColorChanged = { onBackgroundColorChange(Color(it)) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // Save Profile & Continue CTA
        TactilePrimaryButton(
            text = stringResource(R.string.save_profile_continue),
            onClick = onSaveProfile,
            enabled = profileState.editedUserName.isNotBlank()
        )

        Spacer(modifier = Modifier.height(Spacing.xs))
    }
}

