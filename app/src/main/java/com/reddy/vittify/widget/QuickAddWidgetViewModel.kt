package com.reddy.vittify.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.nlp.NlpTransactionParser
import com.reddy.vittify.data.nlp.ParsedTransactionDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickAddWidgetViewModel @Inject constructor(
    private val nlpParser: NlpTransactionParser
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _parsedDraft = MutableStateFlow<ParsedTransactionDraft?>(null)
    val parsedDraft: StateFlow<ParsedTransactionDraft?> = _parsedDraft.asStateFlow()

    fun parseInput(input: String) {
        if (input.isBlank()) return
        
        viewModelScope.launch {
            _isProcessing.update { true }
            try {
                val draft = nlpParser.parse(input)
                _parsedDraft.value = draft
            } catch (e: Exception) {
                // If parsing fails completely, just emit an empty draft to proceed to add screen
                _parsedDraft.value = ParsedTransactionDraft(notes = input)
            } finally {
                _isProcessing.update { false }
            }
        }
    }
}
