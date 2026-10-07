package com.reddy.vittify.presentation.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.reddy.vittify.R
import com.reddy.vittify.data.service.AttachmentService
import com.reddy.vittify.presentation.ui.icons.Camera
import com.reddy.vittify.presentation.ui.icons.Folder2
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.Paperclip2
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import com.reddy.vittify.presentation.ui.theme.rememberAppHapticFeedback
import java.io.File

/**
 * A composable for picking, scanning, capturing, and displaying attachments.
 * Supports on-device ML Kit document scanning with automatic fallback to standard camera capture.
 *
 * @param attachments List of attachment relative paths
 * @param attachmentService The AttachmentService for file operations
 * @param onAddAttachment Callback when a new attachment is added (receives relative path)
 * @param onRemoveAttachment Callback when an attachment is removed
 * @param onAttachmentClick Callback when an attachment is clicked for viewing
 * @param modifier Modifier for the composable
 * @param isEditable Whether attachments can be added/removed
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSection(
    attachments: List<String>,
    attachmentService: AttachmentService,
    onAddAttachment: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onAttachmentClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isEditable: Boolean = true
) {
    val context = LocalContext.current
    val haptic = rememberAppHapticFeedback()
    var pendingCameraCapture by remember { mutableStateOf<Pair<Uri, File>?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    // Unified preview handler
    val handleAttachmentClick: (String) -> Unit = { path ->
        onAttachmentClick(path)
        val uri = attachmentService.getAttachmentUri(path)
        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, attachmentService.getAttachmentMimeType(path))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    // 1. Storage file picker launcher
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            attachmentService.saveAttachment(it, 0L)?.let { path ->
                haptic.click()
                onAddAttachment(path)
            }
        }
    }

    // 2. Camera capture fallback launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capture = pendingCameraCapture
        if (success && capture != null) {
            haptic.click()
            attachmentService.saveAttachment(capture.first, 0L)?.let { path ->
                onAddAttachment(path)
            }
        }
        capture?.second?.delete()
        pendingCameraCapture = null
    }

    val launchCamera: () -> Unit = {
        val capturePair = attachmentService.createTempCaptureUri()
        if (capturePair != null) {
            pendingCameraCapture = capturePair
            try {
                cameraLauncher.launch(capturePair.first)
            } catch (e: Exception) {
                capturePair.second.delete()
                pendingCameraCapture = null
                Toast.makeText(context, R.string.camera_unavailable, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.camera_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    // 3. ML Kit Document Scanner launcher
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                try {
                    val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    if (scanResult != null) {
                        haptic.click()
                        val pages = scanResult.pages
                        if (!pages.isNullOrEmpty()) {
                            pages.forEach { page ->
                                attachmentService.saveAttachment(page.imageUri, 0L)?.let { path ->
                                    onAddAttachment(path)
                                }
                            }
                        } else {
                            scanResult.pdf?.uri?.let { pdfUri ->
                                attachmentService.saveAttachment(pdfUri, 0L)?.let { path ->
                                    onAddAttachment(path)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val launchScanner: () -> Unit = {
        val activity = context.findActivity()
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
                    .addOnFailureListener {
                        // ML Kit unavailable: fallback to standard camera
                        launchCamera()
                    }
            } catch (e: Exception) {
                // If client init fails: fallback to camera
                launchCamera()
            }
        } else {
            launchCamera()
        }
    }

    val launchFilePicker: () -> Unit = {
        filePicker.launch(
            arrayOf(
                "image/*",
                "application/pdf",
                "text/csv",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Header with tactile dual-action group
        ListItem(
            headline = {
                Text(
                    stringResource(R.string.attachments),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supporting = if (attachments.isNotEmpty()) {
                {
                    Text(
                        pluralStringResource(R.plurals.attachments_count, attachments.size, attachments.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            leading = {
                Icon(
                    Iconax.Paperclip2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailing = if (isEditable) {
                {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Primary tactile action: Scan receipt / Camera capture
                        AttachmentActionButton(
                            onClick = launchScanner,
                            onLongClick = { showOptionsSheet = true },
                            icon = Iconax.Camera,
                            contentDescription = stringResource(R.string.scan_receipt_cd),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Secondary action: Browse storage files
                        AttachmentActionButton(
                            onClick = launchFilePicker,
                            onLongClick = { showOptionsSheet = true },
                            icon = Iconax.Folder2,
                            contentDescription = stringResource(R.string.browse_files_cd),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null,
            listColor = MaterialTheme.colorScheme.surfaceContainerLow,
            padding = PaddingValues(0.dp),
            shape = listSingleItemShape,
        )

        // Attachment previews
        if (attachments.isNotEmpty()) {
            val count = attachments.size
            if (count <= 3) {
                // Spread layout for few items
                val itemHeight = if (count == 1) 200.dp else 140.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    attachments.forEach { attachment ->
                        AttachmentPreviewItem(
                            attachmentPath = attachment,
                            attachmentService = attachmentService,
                            onClick = { handleAttachmentClick(attachment) },
                            onRemove = if (isEditable) {
                                { onRemoveAttachment(attachment) }
                            } else null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            } else {
                // Scrollable layout for many items
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(horizontal = Spacing.md)
                ) {
                    items(attachments) { attachment ->
                        AttachmentPreviewItem(
                            attachmentPath = attachment,
                            attachmentService = attachmentService,
                            onClick = { handleAttachmentClick(attachment) },
                            onRemove = if (isEditable) {
                                { onRemoveAttachment(attachment) }
                            } else null,
                            modifier = Modifier.size(100.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal options bottom sheet (accessible via long-press on action buttons)
    if (showOptionsSheet) {
        VittifyModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            shape = VittifyShapes.bottomSheet,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = stringResource(R.string.add_attachment),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                )

                ListItem(
                    headline = { Text(stringResource(R.string.scan_receipt)) },
                    supporting = { Text(stringResource(R.string.scan_receipt_desc)) },
                    leading = {
                        Icon(
                            Iconax.Camera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        showOptionsSheet = false
                        launchScanner()
                    },
                    listColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )

                ListItem(
                    headline = { Text(stringResource(R.string.take_photo)) },
                    supporting = { Text(stringResource(R.string.take_photo_desc)) },
                    leading = {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        showOptionsSheet = false
                        launchCamera()
                    },
                    listColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )

                ListItem(
                    headline = { Text(stringResource(R.string.browse_files)) },
                    supporting = { Text(stringResource(R.string.browse_files_desc)) },
                    leading = {
                        Icon(
                            Iconax.Folder2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        showOptionsSheet = false
                        launchFilePicker()
                    },
                    listColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AttachmentActionButton(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val haptic = rememberAppHapticFeedback()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(MaterialTheme.shapes.largeIncreased)
            .background(containerColor)
            .combinedClickable(
                role = Role.Button,
                onClick = {
                    haptic.click()
                    onClick()
                },
                onLongClick = if (onLongClick != null) {
                    {
                        haptic.longClick()
                        onLongClick()
                    }
                } else null
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
private fun AttachmentPreviewItem(
    attachmentPath: String,
    attachmentService: AttachmentService,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isImage = attachmentService.isImage(attachmentPath)
    val fileName = attachmentPath.substringAfterLast('/')
    val fileUri = attachmentService.getAttachmentUri(attachmentPath)
    val isFileExists = fileUri != null

    Card(
        modifier = modifier
            .clickable(
                enabled = isFileExists,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = VittifyShapes.scaled(Dimensions.Radius.md),
        border = VittifySurface.platterBorder(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFileExists) VittifySurface.surfaceContainerHighColor() 
                           else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isImage && isFileExists) {
                // Image preview
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fileUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Document icon or Error
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.sm),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isFileExists) {
                        Icon(
                            imageVector = when {
                                attachmentPath.endsWith(".pdf") -> Icons.Default.PictureAsPdf
                                attachmentPath.endsWith(".csv") || 
                                attachmentPath.endsWith(".xls") ||
                                attachmentPath.endsWith(".xlsx") -> Icons.Default.TableChart
                                else -> Icons.Default.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fileName.takeLast(15),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        // Error State
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = stringResource(R.string.file_missing),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.missing),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Remove button (always enabled to allow cleanup)
            if (onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_cd),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
