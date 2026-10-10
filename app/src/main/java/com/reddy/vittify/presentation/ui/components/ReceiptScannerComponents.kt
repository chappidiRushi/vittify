package com.reddy.vittify.presentation.ui.components

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.ReceiptSearch
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback
import com.reddy.vittify.utils.ReceiptImageUtils

/**
 * Remembers a launcher function for ML Kit Document Scanner configured for receipts.
 */
@Composable
fun rememberReceiptScannerLauncher(
    onImageScanned: (Uri) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val haptic = rememberAppHapticFeedback()

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                try {
                    val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    val firstPage = scanResult?.pages?.firstOrNull()?.imageUri
                        ?: scanResult?.pdf?.uri
                    if (firstPage != null) {
                        haptic.click()
                        onImageScanned(firstPage)
                    } else {
                        android.util.Log.e("ReceiptScanner", "No image or PDF URI found in GmsDocumentScanningResult: $scanResult")
                        Toast.makeText(context, R.string.receipt_scan_failed_title, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReceiptScanner", "Error extracting scan result", e)
                    Toast.makeText(context, R.string.receipt_scan_failed_title, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    return {
        val activity = ReceiptImageUtils.findActivity(context)
        if (activity != null) {
            try {
                val options = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(false)
                    .setPageLimit(1)
                    .setResultFormats(
                        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                        GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                    )
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .build()

                val scanner = GmsDocumentScanning.getClient(options)
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("ReceiptScanner", "Failed to start document scan intent", e)
                        Toast.makeText(context, R.string.camera_unavailable, Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                android.util.Log.e("ReceiptScanner", "Client initialization failed", e)
                Toast.makeText(context, R.string.camera_unavailable, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.camera_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Material 3 Expressive squircle platter card offering the "Scan Receipt" action.
 */
@Composable
fun ReceiptScanPlatter(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = VittifyShapes.input,
        color = VittifySurface.surfaceContainerLowColor(),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Iconax.ReceiptSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(R.string.scan_receipt),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.scan_receipt_ai_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Progress dialog displayed while Gemini AI is analyzing a scanned receipt.
 */
@Composable
fun ReceiptScanningProgressDialog(
    isVisible: Boolean
) {
    if (isVisible) {
        Dialog(onDismissRequest = { /* Non-cancellable during processing */ }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(VittifyShapes.dialog),
                colors = CardDefaults.cardColors(
                    containerColor = VittifySurface.surfaceContainerLowColor()
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        text = stringResource(R.string.analyzing_receipt_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.analyzing_receipt_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm)
                    )
                }
            }
        }
    }
}

/**
 * Alert dialog shown when user attempts to scan a receipt but Gemini AI is not yet configured.
 */
@Composable
fun AiNotConfiguredDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNavigateToAiSettings: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.ai_not_configured_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.ai_not_configured_receipt_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToAiSettings()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.open_ai_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = VittifyShapes.dialog
        )
    }
}

/**
 * Error dialog shown when receipt analysis fails.
 */
@Composable
fun ReceiptScanErrorDialog(
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    if (!errorMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.receipt_scan_failed_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.ok))
                }
            },
            shape = VittifyShapes.dialog
        )
    }
}
