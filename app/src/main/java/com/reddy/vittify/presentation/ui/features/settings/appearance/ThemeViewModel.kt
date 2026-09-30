package com.reddy.vittify.presentation.ui.features.settings.appearance

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.preferences.NavigationBarStyle
import com.reddy.vittify.data.preferences.AppFont
import com.reddy.vittify.data.preferences.ThemeStyle
import com.reddy.vittify.data.preferences.AccentColor
import com.reddy.vittify.data.preferences.AppIcon
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeUiState: StateFlow<ThemeUiState> = userPreferencesRepository.userPreferences
        .map { preferences ->
            ThemeUiState(
                isDarkTheme = preferences.isDarkThemeEnabled,
                isDynamicColorEnabled = preferences.isDynamicColorEnabled,
                hasSkippedSmsPermission = preferences.hasSkippedSmsPermission,
                isAmoledMode = preferences.isAmoledMode,
                navigationBarStyle = preferences.navigationBarStyle,
                appFont = preferences.appFont,
                customFontPath = preferences.customFontPath,
                customFontName = preferences.customFontName,
                dynamicSeedColor = preferences.dynamicSeedColor,
                themeStyle = preferences.themeStyle,
                accentColor = preferences.accentColor,
                hideNavigationLabels = preferences.hideNavigationLabels,
                hidePillIndicator = preferences.hidePillIndicator,
                profileSwitcherInFooter = preferences.profileSwitcherInFooter,
                blurEffects = preferences.blurEffects,
                isPlayfulAnimationEnabled = preferences.isPlayfulAnimationEnabled,
                backgroundStyle = preferences.backgroundStyle,
                backgroundOpacity = preferences.backgroundOpacity,
                surfaceOpacity = preferences.surfaceOpacity,
                borderThickness = preferences.borderThickness,
                borderOpacity = preferences.borderOpacity,
                cornerRadiusScale = preferences.cornerRadiusScale,
                paddingScale = preferences.paddingScale,
                isOnboardingFinished = preferences.hasShownScanTutorial,
                currentAppIcon = preferences.appIcon,
                isLoaded = true
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeUiState(isLoaded = false)
        )

    fun updateDarkTheme(enabled: Boolean?) {
        viewModelScope.launch {
            userPreferencesRepository.updateDarkThemeEnabled(enabled)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDynamicColorEnabled(enabled)
        }
    }

    fun updateAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateAmoledMode(enabled)
        }
    }

    fun updateNavigationBarStyle(style: NavigationBarStyle) {
        viewModelScope.launch {
            userPreferencesRepository.updateNavigationBarStyle(style)
        }
    }

    fun updateAppFont(font: AppFont) {
        viewModelScope.launch {
            userPreferencesRepository.updateAppFont(font)
        }
    }

    fun updateThemeStyle(style: ThemeStyle) {
        viewModelScope.launch {
            userPreferencesRepository.updateThemeStyle(style)
        }
    }

    fun updateAccentColor(color: AccentColor) {
        viewModelScope.launch {
            userPreferencesRepository.updateAccentColor(color)
        }
    }

    fun updateHideNavigationLabels(hide: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateHideNavigationLabels(hide)
        }
    }

    fun updateHidePillIndicator(hide: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateHidePillIndicator(hide)
        }
    }

    fun updateProfileSwitcherInFooter(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateProfileSwitcherInFooter(enabled)
        }
    }

    fun updateBlurEffects(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateBlurEffects(enabled)
        }
    }

    fun updatePlayfulAnimation(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updatePlayfulAnimationEnabled(enabled)
        }
    }

    fun updateBackgroundStyle(style: String) {
        viewModelScope.launch {
            userPreferencesRepository.setBackgroundStyle(style)
        }
    }

    fun updateBackgroundOpacity(opacity: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setBackgroundOpacity(opacity)
        }
    }

    fun updateAppIcon(icon: AppIcon) = viewModelScope.launch {
        userPreferencesRepository.updateAppIcon(icon)
    }

    fun updateDynamicSeedColor(color: Int) = viewModelScope.launch {
        userPreferencesRepository.updateDynamicSeedColor(color)
    }

    fun importFontFromUri(context: android.content.Context, uri: android.net.Uri, fontName: String) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val fontsDir = java.io.File(context.filesDir, "fonts").apply { mkdirs() }
            val targetFile = java.io.File(fontsDir, "custom_font.ttf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            userPreferencesRepository.updateCustomFont(targetFile.absolutePath, fontName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSurfaceOpacity(opacity: Float) = viewModelScope.launch {
        userPreferencesRepository.updateSurfaceOpacity(opacity)
    }

    fun updateBorderThickness(thickness: Float) = viewModelScope.launch {
        userPreferencesRepository.updateBorderThickness(thickness)
    }

    fun updateBorderOpacity(opacity: Float) = viewModelScope.launch {
        userPreferencesRepository.updateBorderOpacity(opacity)
    }

    fun updateCornerRadiusScale(scale: Float) = viewModelScope.launch {
        userPreferencesRepository.updateCornerRadiusScale(scale)
    }

    fun updatePaddingScale(scale: Float) = viewModelScope.launch {
        userPreferencesRepository.updatePaddingScale(scale)
    }

    fun resetSurfaceTokens() = viewModelScope.launch {
        userPreferencesRepository.updateSurfaceOpacity(1.0f)
        userPreferencesRepository.updateBorderThickness(0.5f)
        userPreferencesRepository.updateBorderOpacity(0.15f)
        userPreferencesRepository.updateCornerRadiusScale(0.75f)
        userPreferencesRepository.updatePaddingScale(1.0f)
    }
}

data class ThemeUiState(
    val isDarkTheme: Boolean? = null, // null = follow system
    val isDynamicColorEnabled: Boolean = false, // Default to custom theme colors
    val hasSkippedSmsPermission: Boolean = false,
    val isAmoledMode: Boolean = false,
    val navigationBarStyle: NavigationBarStyle = NavigationBarStyle.NORMAL,
    val appFont: AppFont = AppFont.SYSTEM,
    val customFontPath: String? = null,
    val customFontName: String? = null,
    val dynamicSeedColor: Int = -1,
    val themeStyle: ThemeStyle = ThemeStyle.DYNAMIC,
    val accentColor: AccentColor = AccentColor.BLUE,
    val hideNavigationLabels: Boolean = false,
    val hidePillIndicator: Boolean = false,
    val profileSwitcherInFooter: Boolean = false,
    val blurEffects: Boolean = true,
    val isPlayfulAnimationEnabled: Boolean = true,
    val backgroundStyle: String = "CONSTELLATION",
    val backgroundOpacity: Float = 0.90f,
    val surfaceOpacity: Float = 1.0f,
    val borderThickness: Float = 0.5f,
    val borderOpacity: Float = 0.15f,
    val cornerRadiusScale: Float = 0.75f,
    val paddingScale: Float = 1.0f,
    val isOnboardingFinished: Boolean = false,
    val currentAppIcon: AppIcon = AppIcon.ORIGINAL,
    val isLoaded: Boolean = false
)