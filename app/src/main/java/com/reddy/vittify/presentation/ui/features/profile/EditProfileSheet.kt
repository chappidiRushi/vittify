package com.reddy.vittify.presentation.ui.features.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.components.ColorPickerContent
import com.reddy.vittify.presentation.ui.icons.Camera
import com.reddy.vittify.presentation.ui.icons.CloseCircle
import com.reddy.vittify.presentation.ui.icons.Edit2
import com.reddy.vittify.presentation.ui.icons.GalleryExport
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.presentation.ui.components.ProfileAvatarImage
import com.reddy.vittify.utils.AvatarUtils

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditProfileSheet(
        state: EditProfileState,
        onNameChange: (String) -> Unit,
        onProfileImageChange: (Uri?) -> Unit,
        onBackgroundColorChange: (Color) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit
) {

    // Launchers for image picking
    val profileImageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) onProfileImageChange(uri)
        }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(VittifyShapes.large)
                .padding(vertical = Spacing.md)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text(
                text = stringResource(R.string.edit_profile),
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
            )

            // Preview Card
            ProfileCardPreview(
                profileImageUri = state.editedProfileImageUri,
                backgroundColor = state.editedProfileBackgroundColor,
                modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
            )

            // Name Input
            TextField(
                value = state.editedUserName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.name_label), fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.Padding.content),
                shape = VittifyShapes.input,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                    unfocusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        0.7f
                    )
                ),
                leadingIcon = { Icon(Iconax.Edit2, contentDescription = null)
                }
            )

            // Profile Image Selection
            Column(
            ) {
                PresetAvatarSelection(
                    selectedUri = state.editedProfileImageUri,
                    onSelect = onProfileImageChange
                )

                Spacer(Modifier.height(Spacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Button(
                        onClick = { profileImageLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = VittifyShapes.scaled(
                            topStart = 16.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp,
                            bottomStart = 16.dp
                        ),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(Iconax.GalleryExport, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.gallery))
                    }

                    Button(
                        onClick = { onProfileImageChange(null) },
                        modifier = Modifier.weight(1f),
                        shape = VittifyShapes.scaled(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 4.dp
                        ),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(Iconax.CloseCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.clear))
                    }
                }
            }

            // Background Color Selection
            Box(

                modifier = Modifier
                    .padding(horizontal = Dimensions.Padding.content)
                    .background(
                        color = VittifySurface.surfaceContainerLowColor(),
                        shape = VittifyShapes.input
                    )
            ) {
                ColorPickerContent(
                    initialColor = state.editedProfileBackgroundColor.toArgb(),
                    onColorChanged = { onBackgroundColorChange(Color(it)) }
                )
            }



            Spacer(Modifier.height(160.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .padding(horizontal = Dimensions.Padding.content)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(56.dp),
                shapes = ButtonDefaults.shapes(),
                enabled = state.hasChanges
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun ProfileCardPreview(
    modifier: Modifier = Modifier,
    profileImageUri: Uri?,
    backgroundColor: Color
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(VittifyShapes.input)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {

        // Profile Image
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, bottom = 16.dp)
                .size(70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            ProfileAvatarImage(
                profileImageUri = profileImageUri,
                contentDescription = stringResource(R.string.profile),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun PresetAvatarSelection(selectedUri: Uri?, onSelect: (Uri) -> Unit) {
    val context = LocalContext.current
    val avatars = remember { AvatarUtils.PRESET_AVATARS }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item { Spacer(Modifier.width(8.dp)) }
        items(avatars) { avatarRes ->
            val avatarUri = remember(avatarRes, context) {
                AvatarUtils.getAvatarUri(context, avatarRes)
            }
            val isSelected = remember(selectedUri, avatarRes, context) {
                AvatarUtils.isAvatarSelected(selectedUri, avatarRes, context)
            }

            Box(modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
                )
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    shape = CircleShape
                )
                .clickable { onSelect(avatarUri) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = avatarRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        item { Spacer(Modifier.width(16.dp)) }
    }
}
