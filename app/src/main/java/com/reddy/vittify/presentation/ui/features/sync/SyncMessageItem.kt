package com.reddy.vittify.presentation.ui.features.sync

import com.reddy.parser.core.ParsedTransaction

data class SyncMessageItem(
    val id: String,
    val parsedTransaction: ParsedTransaction,
    val sender: String,
    val smsBody: String,
    val timestamp: Long,
    val isSelected: Boolean = true
)

