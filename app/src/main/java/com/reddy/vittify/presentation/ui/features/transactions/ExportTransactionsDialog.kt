package com.reddy.vittify.presentation.ui.features.transactions

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.export.ExportResult
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.ImportArrow01
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.LocalBlurEffects
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R

@OptIn(ExperimentalHazeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExportTransactionsDialog(
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    blurEffects: Boolean = LocalBlurEffects.current,
    viewModel: ExportViewModel = hiltViewModel(),
    hazeState: HazeState = remember { HazeState() },
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportState by remember { mutableStateOf<ExportState>(ExportState.Ready) }
    
    Dialog(onDismissRequest = {
        if (exportState !is ExportState.Exporting) {
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(com.reddy.vittify.presentation.ui.theme.VittifyShapes.dialog)
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = HazeDefaults.tint(containerColor),
                                blurRadius = 20.dp,
                                noiseFactor = -1f,
                            )
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                        }
                    ) else Modifier
                ),
            shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.dialog,
            border = com.reddy.vittify.presentation.ui.theme.VittifySurface.platterBorder(),
            colors = CardDefaults.cardColors(
                containerColor = if (blurEffects) com.reddy.vittify.presentation.ui.theme.VittifySurface.surfaceContainerLowColor().copy(0.5f)
                else com.reddy.vittify.presentation.ui.theme.VittifySurface.surfaceContainerLowColor(),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                val icon = when (exportState) {
                    is ExportState.Ready -> Iconax.ImportArrow01
                    is ExportState.Exporting -> Icons.Rounded.HourglassTop
                    is ExportState.Success -> Icons.Rounded.CheckCircle
                    is ExportState.Error -> Icons.Rounded.Error
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = when (exportState) {
                        is ExportState.Success -> MaterialTheme.colorScheme.primary
                        is ExportState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = when (exportState) {
                        is ExportState.Ready -> stringResource(R.string.export_transactions)
                        is ExportState.Exporting -> stringResource(R.string.exporting)
                        is ExportState.Success -> stringResource(R.string.export_complete)
                        is ExportState.Error -> stringResource(R.string.export_failed)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Content based on state
                when (val state = exportState) {
                    is ExportState.Ready -> {
                        Text(
                            text = stringResource(R.string.export_transactions_desc, transactions.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Summary info
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                // Total transactions row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.total_transactions_export),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = transactions.size.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Date range in column layout to prevent wrapping
                                if (transactions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                                    val startDate = transactions.last().dateTime.format(dateFormatter)
                                    val endDate = transactions.first().dateTime.format(dateFormatter)
                                    
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.date_range),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$startDate - $endDate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    is ExportState.Exporting -> {
                        LinearWavyProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    is ExportState.Success -> {
                        Text(
                            text = stringResource(R.string.export_success_msg_format, state.transactionCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // File info
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = state.fileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.size_format, formatFileSize(state.fileSizeBytes)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    is ExportState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    when (exportState) {
                        is ExportState.Ready -> {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(
                                    topStart = Dimensions.Radius.xxl,
                                    topEnd = Dimensions.Radius.xs,
                                    bottomStart = Dimensions.Radius.xxl,
                                    bottomEnd = Dimensions.Radius.xs
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.exportTransactions(transactions).collect { result ->
                                            when (result) {
                                                is ExportResult.Progress -> {
                                                    exportState = ExportState.Exporting(
                                                        progress = result.progress,
                                                        message = result.message
                                                    )
                                                }
                                                is ExportResult.Success -> {
                                                    exportState = ExportState.Success(
                                                        uri = result.uri,
                                                        fileName = result.fileName,
                                                        transactionCount = result.transactionCount,
                                                        fileSizeBytes = result.fileSizeBytes
                                                    )
                                                }
                                                is ExportResult.Error -> {
                                                    exportState = ExportState.Error(
                                                        message = result.message
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(
                                    topStart = Dimensions.Radius.xs,
                                    topEnd = Dimensions.Radius.xxl,
                                    bottomStart = Dimensions.Radius.xs,
                                    bottomEnd = Dimensions.Radius.xxl
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.export))
                            }
                        }
                        
                        is ExportState.Exporting -> {
                            // No buttons during export
                        }
                        
                        is ExportState.Success -> {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(
                                    topStart = Dimensions.Radius.xxl,
                                    topEnd = Dimensions.Radius.xs,
                                    bottomStart = Dimensions.Radius.xxl,
                                    bottomEnd = Dimensions.Radius.xs
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.done))
                            }
                            
                            Button(
                                onClick = {
                                    // Share the exported file
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, (exportState as ExportState.Success).uri)
                                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.csv_export_subject))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_csv)))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(
                                    topStart = Dimensions.Radius.xs,
                                    topEnd = Dimensions.Radius.xxl,
                                    bottomStart = Dimensions.Radius.xs,
                                    bottomEnd = Dimensions.Radius.xxl
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.share))
                            }
                        }
                        
                        is ExportState.Error -> {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(
                                    topStart = Dimensions.Radius.xxl,
                                    topEnd = Dimensions.Radius.xs,
                                    bottomStart = Dimensions.Radius.xxl,
                                    bottomEnd = Dimensions.Radius.xs
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.close))
                            }
                            
                            Button(
                                onClick = {
                                    exportState = ExportState.Ready
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.scaled(
                                    topStart = Dimensions.Radius.xs,
                                    topEnd = Dimensions.Radius.xxl,
                                    bottomStart = Dimensions.Radius.xs,
                                    bottomEnd = Dimensions.Radius.xxl
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class ExportState {
    object Ready : ExportState()
    data class Exporting(val progress: Float, val message: String) : ExportState()
    data class Success(
        val uri: Uri,
        val fileName: String,
        val transactionCount: Int,
        val fileSizeBytes: Long
    ) : ExportState()
    data class Error(val message: String) : ExportState()
}

private fun formatFileSize(bytes: Long): String {
    val df = DecimalFormat("#.##")
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${df.format(bytes / 1024.0)} KB"
        else -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
    }
}