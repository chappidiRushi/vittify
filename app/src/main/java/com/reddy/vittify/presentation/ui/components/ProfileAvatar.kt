package com.reddy.vittify.presentation.ui.components

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback
import com.reddy.vittify.utils.AvatarUtils

/**
 * Renders the image content of a profile avatar.
 * If the uri corresponds to a preset avatar, it is synchronously rendered using painterResource,
 * avoiding any async delay or loading failures across different build packages.
 * If the uri is a custom file/content uri, AsyncImage is used with fallback/error handling.
 * If null, the default avatar (avatar_1) is rendered.
 */
@Composable
fun ProfileAvatarImage(
    profileImageUri: Uri?,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.profile),
    contentScale: ContentScale = ContentScale.Crop,
    @DrawableRes defaultAvatarRes: Int = R.drawable.avatar_1
) {
    val context = LocalContext.current
    val presetRes = remember(profileImageUri, context) {
        AvatarUtils.resolvePresetAvatarRes(context, profileImageUri)
    }

    val isCustomUriValid = profileImageUri != null &&
        profileImageUri.toString().trim().isNotEmpty() &&
        profileImageUri.toString().trim() != "null"

    if (presetRes != null) {
        Image(
            painter = painterResource(id = presetRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (isCustomUriValid) {
        AsyncImage(
            model = profileImageUri,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            error = painterResource(id = defaultAvatarRes),
            fallback = painterResource(id = defaultAvatarRes)
        )
    } else {
        Image(
            painter = painterResource(id = defaultAvatarRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

/**
 * Standard reusable expressive squircle avatar container.
 */
@Composable
fun ProfileAvatar(
    modifier: Modifier = Modifier,
    profileImageUri: Uri?,
    profileBackgroundColor: Color = Color.Transparent,
    contentDescription: String? = stringResource(R.string.profile_desc),
    shape: Shape = RoundedCornerShape(percent = 38),
    onClick: (() -> Unit)? = null
) {
    val haptic = rememberAppHapticFeedback()
    val clickableModifier = if (onClick != null) {
        Modifier.clickable {
            haptic.click()
            onClick()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .background(profileBackgroundColor)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        ProfileAvatarImage(
            profileImageUri = profileImageUri,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}
