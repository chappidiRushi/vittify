package com.reddy.vittify.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.reddy.vittify.MainActivity
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.theme.VittifyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickAddWidgetActivity : ComponentActivity() {

    private val viewModel: QuickAddWidgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // This makes the activity finish when clicking outside the content
        setFinishOnTouchOutside(true)

        setContent {
            VittifyTheme {
                QuickAddOverlayScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() },
                    onDraftParsed = { draft ->
                        val intent = Intent(this, MainActivity::class.java).apply {
                            action = MainActivity.ACTION_QUICK_ADD_PREFILL
                            putExtra(MainActivity.EXTRA_NLP_AMOUNT, draft.amount.ifBlank { null })
                            putExtra(MainActivity.EXTRA_NLP_MERCHANT, draft.merchant.ifBlank { null })
                            putExtra(MainActivity.EXTRA_NLP_TYPE, draft.type.name)
                            putExtra(MainActivity.EXTRA_NLP_BANK_NAME, draft.bankName.ifBlank { null })
                            putExtra(MainActivity.EXTRA_NLP_NOTES, draft.notes.ifBlank { null })
                            putExtra(MainActivity.EXTRA_NLP_CATEGORY, draft.category.ifBlank { null })
                            putExtra(MainActivity.EXTRA_NLP_SUBCATEGORY, draft.subcategory.ifBlank { null })
                            putExtra(MainActivity.EXTRA_NLP_DATE, draft.date?.toString())
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun QuickAddOverlayScreen(
    viewModel: QuickAddWidgetViewModel,
    onDismiss: () -> Unit,
    onDraftParsed: (com.reddy.vittify.data.nlp.ParsedTransactionDraft) -> Unit
) {
    val isProcessing by viewModel.isProcessing.collectAsState()
    val parsedDraft by viewModel.parsedDraft.collectAsState()
    
    LaunchedEffect(parsedDraft) {
        parsedDraft?.let {
            onDraftParsed(it)
        }
    }

    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding(),
            shape = VittifyShapes.large,
            color = VittifySurface.surfaceContainerColor(),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.quick_add_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.padding(8.dp))
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("e.g. Spent $5 on coffee") },
                    shape = VittifyShapes.input,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isProcessing) {
                                viewModel.parseInput(inputText)
                            }
                        }
                    ),
                    trailingIcon = {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.parseInput(inputText)
                                    }
                                },
                                enabled = inputText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    enabled = !isProcessing
                )
            }
        }
    }
}
