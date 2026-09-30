package com.reddy.vittify.presentation.ui.features.settings.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.reddy.vittify.R
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.VittifySvgShape
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.CodeCircle
import com.reddy.vittify.presentation.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

data class CreditItem(
    val title: String,
    val author: String,
    val description: String,
    val url: String? = null,
    val tag: String,
    val shape: VittifySvgShape = VittifySvgShape.COOKIE_8,
    val tint: Color = blue_dark,
    val bg: Color = blue_light
)

private val CREDITS_LIST = listOf(
    CreditItem(
        title = "Cashiro",
        author = "modestcat / Ritesh Kanwar",
        description = "Vittify was forked from Cashiro. We express our utmost gratitude to modestcat (Ritesh Kanwar) for creating the original open-source application and establishing the initial design patterns and transaction tracking architecture.",
        url = "https://github.com/modestcat03",
        tag = "Original Project",
        shape = VittifySvgShape.STAR_8,
        tint = red_dark,
        bg = red_light
    ),
    CreditItem(
        title = "Jetpack Compose & Material 3 Expressive",
        author = "Google & Android Open Source Project",
        description = "Modern UI toolkit, squircle geometry, spring physics, and Material 3 design foundation.",
        url = "https://developer.android.com/jetpack/compose",
        tag = "Core Framework",
        shape = VittifySvgShape.SCALLOP_12,
        tint = green_dark,
        bg = green_light
    ),
    CreditItem(
        title = "Haze",
        author = "Chris Banes",
        description = "High-performance glassmorphic background blurs and blur effects for Jetpack Compose.",
        url = "https://github.com/chrisbanes/haze",
        tag = "Graphics",
        shape = VittifySvgShape.COOKIE_8,
        tint = purple_dark,
        bg = purple_light
    ),
    CreditItem(
        title = "ColorPicker Compose",
        author = "Jaewoong Eum (skydoves)",
        description = "Lightweight HSV and palette color picker for Compose themes and custom account styling.",
        url = "https://github.com/skydoves/colorpicker-compose",
        tag = "UI Component",
        shape = VittifySvgShape.PENTAGON,
        tint = orange_dark,
        bg = orange_light
    ),
    CreditItem(
        title = "Stream WebRTC Android",
        author = "Stream.io",
        description = "Robust WebRTC bindings powering encrypted, serverless Peer-to-Peer (P2P) device pairing and Couple Tracker.",
        url = "https://github.com/getstream/webrtc-android",
        tag = "Networking",
        shape = VittifySvgShape.TILTED_PILL,
        tint = blue_dark,
        bg = blue_light
    ),
    CreditItem(
        title = "ZXing ('Zebra Crossing')",
        author = "Sean Owen & ZXing Authors",
        description = "Multi-format 1D/2D barcode image processing and QR code pairing.",
        url = "https://github.com/zxing/zxing",
        tag = "Utilities",
        shape = VittifySvgShape.CLOVER_4,
        tint = cyan_dark,
        bg = cyan_light
    ),
    CreditItem(
        title = "PDFBox-Android",
        author = "Tom Roush & Apache Software Foundation",
        description = "Port of Apache PDFBox library enabling local, on-device financial statement PDF report generation.",
        url = "https://github.com/TomRoush/PdfBox-Android",
        tag = "Document Engine",
        shape = VittifySvgShape.TILTED_OVAL,
        tint = red_dark,
        bg = red_light
    ),
    CreditItem(
        title = "OpenCSV",
        author = "Sean Connolly & Contributors",
        description = "Fast, clean CSV data export parser allowing seamless financial spreadsheet exports.",
        url = "https://opencsv.sourceforge.net/",
        tag = "Data Export",
        shape = VittifySvgShape.SCALLOP_12,
        tint = green_dark,
        bg = green_light
    ),
    CreditItem(
        title = "Coil 3",
        author = "Coil Contributors",
        description = "Kotlin-first, coroutine-based modern image loading library for Android and Compose.",
        url = "https://github.com/coil-kt/coil",
        tag = "Image Loading",
        shape = VittifySvgShape.COOKIE_8,
        tint = orange_dark,
        bg = orange_light
    ),
    CreditItem(
        title = "Kotlin Coroutines, Serialization & Ktor",
        author = "JetBrains",
        description = "First-class asynchronous coroutine flows, type-safe JSON serialization, and lightweight HTTP client engines.",
        url = "https://kotlinlang.org/",
        tag = "Language & Runtime",
        shape = VittifySvgShape.PENTAGON,
        tint = purple_dark,
        bg = purple_light
    ),
    CreditItem(
        title = "Iconax & Material Symbols",
        author = "Design Contributors",
        description = "Refined outline and filled vector iconography ensuring optical consistency across the entire UI.",
        url = null,
        tag = "Iconography",
        shape = VittifySvgShape.STAR_8,
        tint = cyan_dark,
        bg = cyan_light
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.credits),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .overScrollVertical()
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = paddingValues.calculateTopPadding(),
                    bottom = 24.dp
                ),
            flingBehavior = rememberOverscrollFlingBehavior { listState },
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Hero Acknowledgments Card
            item(key = "credits_hero") {
                Surface(
                    shape = VittifyShapes.hero,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Standing on the Shoulders of Giants",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "Vittify is created with profound appreciation for the open-source community, whose passionate work makes modern software possible.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Cashiro Special Platter
            item(key = "cashiro_special") {
                val cashiro = CREDITS_LIST.first()
                Surface(
                    shape = VittifyShapes.platter,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = VittifyShapes.platter
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = cashiro.bg,
                                            shape = cashiro.shape.composeShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = cashiro.tint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = cashiro.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "by ${cashiro.author}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = VittifyShapes.pill,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = cashiro.tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = cashiro.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )

                        if (cashiro.url != null) {
                            Row(
                                modifier = Modifier
                                    .clip(VittifyShapes.pill)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, cashiro.url.toUri())
                                        context.startActivity(intent)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "View Original Author",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Other Libraries & Components
            items(CREDITS_LIST.drop(1), key = { it.title }) { item ->
                Surface(
                    shape = VittifyShapes.platter,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            shape = VittifyShapes.platter
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = item.bg,
                                            shape = item.shape.composeShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Iconax.CodeCircle,
                                        contentDescription = null,
                                        tint = item.tint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.author,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = VittifyShapes.pill,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = item.tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        if (item.url != null) {
                            Row(
                                modifier = Modifier
                                    .clip(VittifyShapes.pill)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, item.url.toUri())
                                        context.startActivity(intent)
                                    }
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Learn more",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Footer
            item(key = "credits_footer") {
                Text(
                    text = "Thank you to every developer and designer whose contributions shaped Vittify.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.md)
                )
            }
        }
    }
}

