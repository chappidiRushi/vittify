package com.reddy.vittify.presentation.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class ToastData(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMillis: Long = 3200L
)

object ToastManager {
    private val _currentToast = MutableStateFlow<ToastData?>(null)
    val currentToast: StateFlow<ToastData?> = _currentToast.asStateFlow()

    fun show(
        message: String,
        type: ToastType = ToastType.INFO,
        durationMillis: Long = 3200L
    ) {
        if (message.isBlank()) return
        _currentToast.value = ToastData(
            id = System.currentTimeMillis(),
            message = message,
            type = type,
            durationMillis = durationMillis
        )
    }

    fun showSuccess(message: String, durationMillis: Long = 3200L) {
        show(message, ToastType.SUCCESS, durationMillis)
    }

    fun showError(message: String, durationMillis: Long = 4000L) {
        show(message, ToastType.ERROR, durationMillis)
    }

    fun showWarning(message: String, durationMillis: Long = 3500L) {
        show(message, ToastType.WARNING, durationMillis)
    }

    fun dismiss() {
        _currentToast.value = null
    }
}
