package com.reddy.vittify.presentation.ui.features.settings.appearance

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.reddy.vittify.R
import com.reddy.vittify.data.preferences.*
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.theme.*
import com.reddy.vittify.utils.IconSwitchingUtils
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val haptic = rememberAppHapticFeedback()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Themes & Colors", "Navigation & Icons", "Effects & Geometry")

    // Bottom Sheet States
    var showCuratedPalettesSheet by remember { mutableStateOf(false) }
    val curatedPalettesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showBackgroundEffectsSheet by remember { mutableStateOf(false) }
    val backgroundEffectsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showSurfaceGeometrySheet by remember { mutableStateOf(false) }
    val surfaceGeometrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // HSV Color Picker Dialog State
    var showColorPicker by remember { mutableStateOf(false) }

    val isDark = themeUiState.isDarkTheme ?: isSystemInDarkTheme()

    // Font Picker Launcher
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            var displayName = "Custom Font"
            try {
                context.contentResolver.query(
                    it,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            themeViewModel.importFontFromUri(context, it, displayName)
            themeViewModel.updateAppFont(AppFont.CUSTOM)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.appearance_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var accumulatedDragX by remember { mutableFloatStateOf(0f) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .pointerInput(selectedTabIndex) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (accumulatedDragX < -70f && selectedTabIndex < 2) {
                                    haptic.click()
                                    selectedTabIndex += 1
                                } else if (accumulatedDragX > 70f && selectedTabIndex > 0) {
                                    haptic.click()
                                    selectedTabIndex -= 1
                                }
                                accumulatedDragX = 0f
                            },
                            onDragCancel = {
                                accumulatedDragX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (kotlin.math.abs(dragAmount) > 2f) {
                                    change.consume()
                                    accumulatedDragX += dragAmount
                                }
                            }
                        )
                    }
                    .padding(
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 80.dp
                    )
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Top Segmented Pill Tab Bar
                AppearanceSegmentedTabBar(
                    selectedTab = selectedTabIndex,
                    tabs = tabTitles,
                    onTabSelected = { selectedTabIndex = it }
                )

                // Tab Content with Bank Card Spring Page Transitions
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        val isForward = targetState > initialState
                        val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
                            dampingRatio = 0.62f,
                            stiffness = 340f
                        )
                        val scaleSpring = AppearanceSprings.BankCardBouncyFloat
                        val fadeSpec = spring<Float>(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )

                        if (isForward) {
                            (slideInHorizontally(animationSpec = springSpec) { fullWidth -> fullWidth } +
                             scaleIn(initialScale = 0.96f, animationSpec = scaleSpring) +
                             fadeIn(animationSpec = fadeSpec))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = springSpec) { fullWidth -> -fullWidth / 2 } +
                                    scaleOut(targetScale = 0.96f, animationSpec = scaleSpring) +
                                    fadeOut(animationSpec = fadeSpec)
                                )
                        } else {
                            (slideInHorizontally(animationSpec = springSpec) { fullWidth -> -fullWidth } +
                             scaleIn(initialScale = 0.96f, animationSpec = scaleSpring) +
                             fadeIn(animationSpec = fadeSpec))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = springSpec) { fullWidth -> fullWidth / 2 } +
                                    scaleOut(targetScale = 0.96f, animationSpec = scaleSpring) +
                                    fadeOut(animationSpec = fadeSpec)
                                )
                        }.using(
                            SizeTransform(clip = false) { _, _ ->
                                spring(
                                    dampingRatio = 0.62f,
                                    stiffness = 340f
                                )
                            }
                        )
                    },
                    label = "appearance_tab_spring_transition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> ThemesAndColorsTab(
                            themeUiState = themeUiState,
                            isDark = isDark,
                            themeViewModel = themeViewModel,
                            onOpenColorPicker = { showColorPicker = true },
                            onOpenCuratedPalettesSheet = { showCuratedPalettesSheet = true }
                        )
                        1 -> NavigationAndIconsTab(
                            themeUiState = themeUiState,
                            themeViewModel = themeViewModel,
                            context = context
                        )
                        2 -> EffectsAndGeometryTab(
                            themeUiState = themeUiState,
                            themeViewModel = themeViewModel,
                            onOpenBackgroundEffectsSheet = { showBackgroundEffectsSheet = true },
                            onOpenSurfaceGeometrySheet = { showSurfaceGeometrySheet = true },
                            onPickFontFromDevice = { fontPickerLauncher.launch(arrayOf("*/*")) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets
    if (showCuratedPalettesSheet) {
        CuratedPalettesSheet(
            sheetState = curatedPalettesSheetState,
            selectedAccent = themeUiState.accentColor,
            isDark = isDark,
            onAccentSelected = { accent ->
                themeViewModel.updateThemeStyle(ThemeStyle.DEFAULT)
                themeViewModel.updateAccentColor(accent)
            },
            onDismiss = { showCuratedPalettesSheet = false }
        )
    }

    if (showBackgroundEffectsSheet) {
        BackgroundEffectsSheet(
            sheetState = backgroundEffectsSheetState,
            currentStyle = themeUiState.backgroundStyle,
            opacity = themeUiState.backgroundOpacity,
            onStyleSelected = { themeViewModel.updateBackgroundStyle(it) },
            onOpacityChanged = { themeViewModel.updateBackgroundOpacity(it) },
            onDismiss = { showBackgroundEffectsSheet = false }
        )
    }

    if (showSurfaceGeometrySheet) {
        SurfaceGeometrySheet(
            sheetState = surfaceGeometrySheetState,
            surfaceOpacity = themeUiState.surfaceOpacity,
            borderThickness = themeUiState.borderThickness,
            borderOpacity = themeUiState.borderOpacity,
            cornerRadiusScale = themeUiState.cornerRadiusScale,
            paddingScale = themeUiState.paddingScale,
            onSurfaceOpacityChange = { themeViewModel.updateSurfaceOpacity(it) },
            onBorderThicknessChange = { themeViewModel.updateBorderThickness(it) },
            onBorderOpacityChange = { themeViewModel.updateBorderOpacity(it) },
            onCornerRadiusScaleChange = { themeViewModel.updateCornerRadiusScale(it) },
            onPaddingScaleChange = { themeViewModel.updatePaddingScale(it) },
            onResetDefaults = { themeViewModel.resetSurfaceTokens() },
            onDismiss = { showSurfaceGeometrySheet = false }
        )
    }

    // HSV Color Picker Dialog for Custom Dynamic Seed
    if (showColorPicker) {
        var pickerColor by remember {
            mutableStateOf(
                if (themeUiState.dynamicSeedColor != -1) Color(themeUiState.dynamicSeedColor)
                else Color(0xFF4F46E5)
            )
        }
        val controller = rememberColorPickerController()
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(stringResource(R.string.pick_seed_color)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(VittifyShapes.input)
                            .background(pickerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#" + Integer.toHexString(pickerColor.toArgb()).uppercase().takeLast(6),
                            color = if (pickerColor.luminance() > 0.5f) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        controller = controller,
                        onColorChanged = { envelope ->
                            pickerColor = envelope.color
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        themeViewModel.updateDynamicSeedColor(pickerColor.toArgb())
                        themeViewModel.updateThemeStyle(ThemeStyle.DYNAMIC)
                        showColorPicker = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Tab 1: Themes & Colors
 */
@Composable
private fun ThemesAndColorsTab(
    themeUiState: ThemeUiState,
    isDark: Boolean,
    themeViewModel: ThemeViewModel,
    onOpenColorPicker: () -> Unit,
    onOpenCuratedPalettesSheet: () -> Unit
) {
    val haptic = rememberAppHapticFeedback()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Platter 1: Theme Mode
        AppearancePlatter(
            title = "Theme Mode",
            subtitle = "Choose light, dark, or follow system setting"
        ) {
            ThemeModeSegmentedControl(
                selectedMode = themeUiState.isDarkTheme,
                onModeSelected = { themeViewModel.updateDarkTheme(it) }
            )

            if (themeUiState.isDarkTheme != false) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                PreferenceSwitch(
                    title = stringResource(R.string.amoled_black),
                    subtitle = stringResource(R.string.amoled_black_desc),
                    checked = themeUiState.isAmoledMode,
                    onCheckedChange = { themeViewModel.updateAmoledMode(it) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    padding = PaddingValues(0.dp),
                    listColor = Color.Transparent,
                    useDefaultBorder = false
                )
            }
        }

        // Platter 2: Color Palette
        val isDynamic = themeUiState.themeStyle == ThemeStyle.DYNAMIC
        AppearancePlatter(
            title = "Color Palette",
            subtitle = if (isDynamic) "Material You wallpaper or seed palette"
            else "Curated theme: ${getAccentThemeName(themeUiState.accentColor)}"
        ) {
            TwoWaySegmentedPill(
                firstOption = stringResource(R.string.style_dynamic),
                secondOption = "Curated",
                isFirstSelected = isDynamic,
                onFirstSelected = { themeViewModel.updateThemeStyle(ThemeStyle.DYNAMIC) },
                onSecondSelected = { themeViewModel.updateThemeStyle(ThemeStyle.DEFAULT) },
                firstIcon = Icons.Rounded.Palette,
                secondIcon = Icons.Rounded.ColorLens
            )

            if (isDynamic) {
                // Dynamic Monet Content
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // Wallpaper sync button
                        val isWallpaperSelected = themeUiState.dynamicSeedColor == -1
                        Button(
                            onClick = {
                                haptic.click()
                                themeViewModel.updateDynamicSeedColor(-1)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .springPress(0.95f),
                            shape = VittifyShapes.input,
                            border = if (!isWallpaperSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)) else null,
                            colors = if (isWallpaperSelected) ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) else ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Rounded.Wallpaper, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = stringResource(R.string.dynamic_wallpaper),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }

                        // Custom seed color button
                        val isCustomSeed = themeUiState.dynamicSeedColor != -1
                        Button(
                            onClick = {
                                haptic.click()
                                onOpenColorPicker()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .springPress(0.95f),
                            shape = VittifyShapes.input,
                            border = if (!isCustomSeed) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)) else null,
                            colors = if (isCustomSeed) ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) else ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isCustomSeed) Color(themeUiState.dynamicSeedColor)
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = stringResource(R.string.dynamic_custom_seed),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }

                    // Preset seed dots
                    val presetSeeds = remember {
                        listOf(
                            Color(0xFF4F46E5), // Indigo
                            Color(0xFF0D9488), // Teal
                            Color(0xFF059669), // Emerald
                            Color(0xFFD97706), // Amber
                            Color(0xFFDC2626), // Crimson
                            Color(0xFFE11D48), // Rose
                            Color(0xFF7C3AED), // Violet
                            Color(0xFF0891B2), // Cyan
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(presetSeeds) { seed ->
                            val isCurrent = themeUiState.dynamicSeedColor == seed.toArgb()
                            val seedShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(seedShape)
                                    .background(seed)
                                    .springPress(targetScale = 0.92f) {
                                        haptic.click()
                                        themeViewModel.updateDynamicSeedColor(seed.toArgb())
                                    }
                                    .then(
                                        if (isCurrent) {
                                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, seedShape)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isCurrent,
                                    enter = scaleIn(initialScale = 0f, animationSpec = AppearanceSprings.BankCardBouncyFloat) + fadeIn(),
                                    exit = scaleOut(targetScale = 0f, animationSpec = AppearanceSprings.PressSpringFloat) + fadeOut()
                                ) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Curated Themes Preview
                val previewAccents = remember {
                    listOf(
                        AccentColor.PINE_ROSE,
                        AccentColor.TEAL,
                        AccentColor.LAVENDER,
                        AccentColor.SAPPHIRE,
                        AccentColor.PEACH,
                        AccentColor.GREEN
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(previewAccents) { accent ->
                            CuratedPaletteCard(
                                accent = accent,
                                isDark = isDark,
                                isSelected = themeUiState.accentColor == accent,
                                onClick = {
                                    haptic.click()
                                    themeViewModel.updateThemeStyle(ThemeStyle.DEFAULT)
                                    themeViewModel.updateAccentColor(accent)
                                },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            haptic.click()
                            onOpenCuratedPalettesSheet()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .springPress(0.96f),
                        shape = VittifyShapes.button,
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Browse All 24 Palettes")
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Navigation & Icons
 */
@Composable
private fun NavigationAndIconsTab(
    themeUiState: ThemeUiState,
    themeViewModel: ThemeViewModel,
    context: android.content.Context
) {
    val haptic = rememberAppHapticFeedback()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Platter 1: Navigation Style
        val isFloating = themeUiState.navigationBarStyle == NavigationBarStyle.FLOATING
        AppearancePlatter(
            title = stringResource(R.string.navigation_style),
            subtitle = "Switch between floating bar and docked M3 navigation",
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md)
            ) {
                TwoWaySegmentedPill(
                    firstOption = stringResource(R.string.nav_floating),
                    secondOption = stringResource(R.string.nav_normal),
                    isFirstSelected = isFloating,
                    onFirstSelected = { themeViewModel.updateNavigationBarStyle(NavigationBarStyle.FLOATING) },
                    onSecondSelected = { themeViewModel.updateNavigationBarStyle(NavigationBarStyle.NORMAL) },
                    firstIcon = Icons.Rounded.ViewCarousel,
                    secondIcon = Icons.Rounded.Dock
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                thickness = 1.dp
            )

            if (!isFloating) {
                PreferenceSwitch(
                    title = stringResource(R.string.hide_nav_labels),
                    subtitle = stringResource(R.string.hide_nav_labels_desc),
                    checked = themeUiState.hideNavigationLabels,
                    onCheckedChange = { themeViewModel.updateHideNavigationLabels(it) },
                    padding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                    listColor = Color.Transparent,
                    useDefaultBorder = false
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                    thickness = 1.dp
                )
                PreferenceSwitch(
                    title = stringResource(R.string.hide_pill_indicator),
                    subtitle = stringResource(R.string.hide_pill_indicator_desc),
                    checked = themeUiState.hidePillIndicator,
                    onCheckedChange = { themeViewModel.updateHidePillIndicator(it) },
                    padding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                    listColor = Color.Transparent,
                    useDefaultBorder = false
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                    thickness = 1.dp
                )
            }

            PreferenceSwitch(
                title = stringResource(R.string.move_profile_switcher_to_footer),
                subtitle = stringResource(R.string.move_profile_switcher_to_footer_desc),
                checked = themeUiState.profileSwitcherInFooter,
                onCheckedChange = { themeViewModel.updateProfileSwitcherInFooter(it) },
                padding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                listColor = Color.Transparent,
                useDefaultBorder = false
            )
        }

        // Platter 2: App Logo
        AppearancePlatter(
            title = "App Logo",
            subtitle = "Customize the home screen launcher icon"
        ) {
            val appIcons = listOf(
                Pair(stringResource(R.string.logo_original), AppIcon.ORIGINAL to R.drawable.vittify_original),
                Pair(stringResource(R.string.logo_anarchy), AppIcon.ANARCHY to R.drawable.vittify_anarchy),
                Pair(stringResource(R.string.logo_comic), AppIcon.COMIC to R.drawable.vittify_comic),
                Pair(stringResource(R.string.logo_zenith), AppIcon.ZENITH to R.drawable.vittify_zenith),
                Pair(stringResource(R.string.logo_monochrome), AppIcon.MONOCHROME to R.drawable.vittify_pastel)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(appIcons) { (name, iconData) ->
                    val (icon, drawableRes) = iconData
                    AppLogoSquircleItem(
                        name = name,
                        drawableResId = drawableRes,
                        isSelected = themeUiState.currentAppIcon == icon,
                        onClick = {
                            haptic.click()
                            IconSwitchingUtils.switchAppIcon(context, icon)
                            themeViewModel.updateAppIcon(icon)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Tab 3: Effects & Geometry
 */
@Composable
private fun EffectsAndGeometryTab(
    themeUiState: ThemeUiState,
    themeViewModel: ThemeViewModel,
    onOpenBackgroundEffectsSheet: () -> Unit,
    onOpenSurfaceGeometrySheet: () -> Unit,
    onPickFontFromDevice: () -> Unit
) {
    val haptic = rememberAppHapticFeedback()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Platter 1: Visual Effects & Motion
        AppearancePlatter(
            title = "Visual Effects",
            subtitle = "Motion, canvas particles, and glassmorphism"
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PreferenceSwitch(
                    title = stringResource(R.string.blur_effects),
                    subtitle = stringResource(R.string.blur_effects_desc),
                    checked = themeUiState.blurEffects,
                    onCheckedChange = { themeViewModel.updateBlurEffects(it) },
                    padding = PaddingValues(0.dp),
                    listColor = Color.Transparent,
                    useDefaultBorder = false
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                    thickness = 1.dp
                )
            }

            PreferenceSwitch(
                title = stringResource(R.string.playful_animation),
                subtitle = stringResource(R.string.playful_animation_desc),
                checked = themeUiState.isPlayfulAnimationEnabled,
                onCheckedChange = { themeViewModel.updatePlayfulAnimation(it) },
                padding = PaddingValues(0.dp),
                listColor = Color.Transparent,
                useDefaultBorder = false
            )

            if (themeUiState.isPlayfulAnimationEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Button(
                    onClick = {
                        haptic.click()
                        onOpenBackgroundEffectsSheet()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .springPress(0.96f),
                    shape = VittifyShapes.button,
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "Customize Effect: ${themeUiState.backgroundStyle} (${(themeUiState.backgroundOpacity * 100).toInt()}%)"
                    )
                }
            }
        }

        // Platter 2: Typography
        AppearancePlatter(
            title = stringResource(R.string.fonts_title),
            subtitle = "Select app font or load custom typeface"
        ) {
            val availableFonts = remember {
                listOf(
                    FontOptionItem(AppFont.SN_PRO, R.string.font_snpro, R.string.font_snpro_sub, SNProFontFamily),
                    FontOptionItem(AppFont.SYSTEM, R.string.style_default, R.string.font_default_sub, FontFamily.Default),
                    FontOptionItem(AppFont.SANS_SERIF, R.string.font_sans_serif, R.string.font_default_sub, FontFamily.SansSerif),
                    FontOptionItem(AppFont.SERIF, R.string.font_serif, R.string.font_default_sub, FontFamily.Serif),
                    FontOptionItem(AppFont.MONOSPACE, R.string.font_monospace, R.string.font_default_sub, FontFamily.Monospace),
                    FontOptionItem(AppFont.CURSIVE, R.string.font_cursive, R.string.font_default_sub, FontFamily.Cursive)
                )
            }

            val allFonts = availableFonts.toMutableList()
            if (themeUiState.customFontPath != null) {
                allFonts.add(
                    FontOptionItem(
                        font = AppFont.CUSTOM,
                        nameRes = R.string.font_custom,
                        subRes = R.string.font_custom,
                        fontFamily = FontFamily.Default
                    )
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                allFonts.forEachIndexed { index, option ->
                    val isSelected = themeUiState.appFont == option.font
                    val position = ListItemPosition.from(index, allFonts.size)
                    val shape = position.toShape()

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.click()
                                themeViewModel.updateAppFont(option.font)
                            },
                        shape = shape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (option.font == AppFont.CUSTOM && !themeUiState.customFontName.isNullOrBlank())
                                        themeUiState.customFontName!!
                                    else stringResource(option.nameRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = option.fontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(option.subRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = option.fontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Button(
                onClick = {
                    haptic.click()
                    onPickFontFromDevice()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .springPress(0.96f),
                shape = VittifyShapes.button,
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Icon(Icons.Rounded.FontDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = if (!themeUiState.customFontName.isNullOrBlank() && themeUiState.appFont == AppFont.CUSTOM)
                        "Active Font: ${themeUiState.customFontName}"
                    else stringResource(R.string.pick_font_from_device)
                )
            }
        }

        // Platter 3: Surface & Geometry
        AppearancePlatter(
            title = "Surface & Geometry",
            subtitle = "Live platter preview and custom token tuning"
        ) {
            // Live Sample Platter Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(VittifyShapes.hero)
                    .background(VittifySurface.platterCardColor())
                    .then(
                        VittifySurface.platterBorder()?.let { Modifier.border(it, VittifyShapes.hero) }
                            ?: Modifier.border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                                VittifyShapes.hero
                            )
                    )
                    .springPress(targetScale = 0.98f)
                    .padding(Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sample Platter Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Opacity: ${(themeUiState.surfaceOpacity * 100).toInt()}% • Border: ${String.format(java.util.Locale.US, "%.1f", themeUiState.borderThickness)}dp • Radius: ${String.format(java.util.Locale.US, "%.2f", themeUiState.cornerRadiusScale)}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(VittifyShapes.button)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    ) {
                        Text(
                            text = "CTA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = {
                    haptic.click()
                    onOpenSurfaceGeometrySheet()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .springPress(0.96f),
                shape = VittifyShapes.button,
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Tune Geometry & Spacing")
            }
        }
    }
}

