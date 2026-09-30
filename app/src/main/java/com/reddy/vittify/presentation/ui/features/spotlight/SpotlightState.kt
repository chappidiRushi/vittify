package com.reddy.vittify.presentation.ui.features.spotlight

import androidx.compose.ui.geometry.Rect

data class SpotlightState(
    val showTutorial: Boolean = false,
    val shouldShowTutorial: Boolean = false,
    val fabPosition: Rect? = null
)
