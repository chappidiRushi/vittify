package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reddy.vittify.presentation.effects.BlurredAnimatedVisibility

@Composable
fun PreferenceSwitch(
    visible: Boolean = true,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: @Composable () -> Unit = {},
    padding: PaddingValues? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    isSingle: Boolean = false,
    listColor: Color? = null,
    border: BorderStroke? = null,
    useDefaultBorder: Boolean = true
) {
    BlurredAnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ListItem(
            headline = { Text(title) },
            supporting = { if (subtitle.isNotEmpty())Text(subtitle) },
            leading = {
                leadingIcon()
            },
            trailing = {
                val haptic = com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback()
                Switch(
                    checked = checked,
                    onCheckedChange = { newValue ->
                        haptic.toggle()
                        onCheckedChange(newValue)
                    },
                    thumbContent = {
                        Icon(
                            if (checked) Icons.Outlined.Check else Icons.Outlined.Close,
                            "Thumb",
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    },
                )
            },
            shape = when {
                isSingle -> ListItemPosition.Single.toShape()
                isFirst -> ListItemPosition.Top.toShape()
                isLast -> ListItemPosition.Bottom.toShape()
                else -> ListItemPosition.Middle.toShape()
            },
            padding = padding,
            listColor = listColor,
            border = border,
            useDefaultBorder = useDefaultBorder
        )
    }
}