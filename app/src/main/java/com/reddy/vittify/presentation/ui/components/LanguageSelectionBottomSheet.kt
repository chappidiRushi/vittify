package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reddy.vittify.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionBottomSheet(
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val supportedLanguages = listOf(
        "en" to "English",
        "af" to "Afrikaans",
        "ar" to "العربية (Arabic)",
        "az" to "Azərbaycan (Azerbaijani)",
        "bg" to "Български (Bulgarian)",
        "bn" to "বাংলা (Bengali)",
        "bo" to "བོད་སྐད་ (Tibetan)",
        "ca" to "Català (Catalan)",
        "cs" to "Čeština (Czech)",
        "da" to "Dansk (Danish)",
        "de" to "Deutsch (German)",
        "dz" to "རྫོང་ཁ་ (Dzongkha)",
        "el" to "Ελληνικά (Greek)",
        "es" to "Español (Spanish)",
        "fa" to "فارسی (Persian)",
        "fr" to "Français (French)",
        "gu" to "ગુજરાતી (Gujarati)",
        "haw" to "ʻŌlelo Hawaiʻi (Hawaiian)",
        "he" to "עברית (Hebrew)",
        "hi" to "हिन्दी (Hindi)",
        "hr" to "Hrvatski (Croatian)",
        "hu" to "Magyar (Hungarian)",
        "id" to "Bahasa Indonesia (Indonesian)",
        "is" to "Íslenska (Icelandic)",
        "it" to "Italiano (Italian)",
        "ja" to "日本語 (Japanese)",
        "kab" to "Taqbaylit (Kabyle)",
        "kn" to "ಕನ್ನಡ (Kannada)",
        "ks" to "कश्मीरी (Kashmiri)",
        "la" to "Latina (Latin)",
        "ml" to "മലയാളം (Malayalam)",
        "mr" to "मराठी (Marathi)",
        "ne" to "नेपाली (Nepali)",
        "nl" to "Nederlands (Dutch)",
        "no" to "Norsk (Norwegian)",
        "ny" to "Chichewa (Nyanja)",
        "or" to "ଓଡ଼ିଆ (Odia)",
        "os" to "Ирон (Ossetian)",
        "pa" to "ਪੰਜਾਬੀ (Punjabi)",
        "pl" to "Polski (Polish)",
        "pt" to "Português (Portuguese)",
        "ro" to "Română (Romanian)",
        "ru" to "Русский (Russian)",
        "sk" to "Slovenčina (Slovak)",
        "sl" to "Slovenščina (Slovenian)",
        "sv" to "Svenska (Swedish)",
        "ta" to "தமிழ் (Tamil)",
        "te" to "తెలుగు (Telugu)",
        "th" to "ไทย (Thai)",
        "tk" to "Türkmençe (Turkmen)",
        "tr" to "Türkçe (Turkish)",
        "uk" to "Українська (Ukrainian)",
        "ur" to "اردو (Urdu)",
        "uz" to "Oʻzbekcha (Uzbek)",
        "val" to "Valencian",
        "vi" to "Tiếng Việt (Vietnamese)",
        "zh" to "中文 (Chinese)"
    )

    com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = VittifyShapes.bottomSheet,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(1.5.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(supportedLanguages) { index, (code, name) ->
                    val position = ListItemPosition.from(index, supportedLanguages.size)
                    VittifyBottomSheetListItem(
                        title = name,
                        subtitle = code.uppercase(),
                        leadingSymbol = code.take(2).uppercase(),
                        selected = code == selectedLanguageCode,
                        position = position,
                        modifier = Modifier.springPress(),
                        onClick = {
                            onLanguageSelected(code)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
