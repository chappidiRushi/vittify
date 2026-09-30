package com.reddy.vittify.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reddy.vittify.presentation.ui.theme.VittifyShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionBottomSheet(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Select Currency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                availableCurrencies.forEachIndexed { index, currency ->
                    val (symbol, name) = when (currency) {
                        "INR" -> "₹" to "Indian Rupee"
                        "USD" -> "$" to "US Dollar"
                        "AED" -> "د.إ" to "UAE Dirham"
                        "NPR" -> "₨" to "Nepalese Rupee"
                        "ETB" -> "ብር" to "Ethiopian Birr"
                        "EUR" -> "€" to "Euro"
                        "GBP" -> "£" to "British Pound"
                        "CAD" -> "$" to "Canadian Dollar"
                        "AUD" -> "$" to "Australian Dollar"
                        "JPY" -> "¥" to "Japanese Yen"
                        else -> currency.take(2) to currency
                    }

                    VittifyBottomSheetListItem(
                        title = "$currency • $name",
                        leadingSymbol = symbol,
                        selected = currency == selectedCurrency,
                        position = ListItemPosition.from(index, availableCurrencies.size),
                        modifier = Modifier.springPress(),
                        onClick = {
                            onCurrencySelected(currency)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
