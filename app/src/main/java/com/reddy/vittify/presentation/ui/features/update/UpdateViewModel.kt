package com.reddy.vittify.presentation.ui.features.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.update.AppUpdateService
import com.reddy.vittify.data.update.model.AppUpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.reddy.vittify.presentation.ui.util.ToastManager
import com.reddy.vittify.presentation.ui.util.ToastType

data class UpdateUiState(
    val isChecking: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val appUpdateService: AppUpdateService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdate(isManualCheck: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, errorMessage = null) }
            val update = appUpdateService.checkForUpdate(isManualCheck = isManualCheck)
            _uiState.update {
                it.copy(
                    isChecking = false,
                    availableUpdate = update
                )
            }
            if (isManualCheck && update == null) {
                ToastManager.showSuccess("You're already on the latest version")
            }
        }
    }

    fun startUpdate() {
        val update = _uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f, errorMessage = null) }
            val result = appUpdateService.downloadAndInstallApk(update) { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }

            result.onSuccess { apkFile ->
                _uiState.update { it.copy(isDownloading = false, downloadProgress = 1f) }
                appUpdateService.triggerInstall(apkFile)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        errorMessage = error.localizedMessage ?: "Failed to download update"
                    )
                }
                ToastManager.showError(error.localizedMessage ?: "Failed to download update")
            }
        }
    }

    fun skipThisUpdate() {
        val update = _uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            appUpdateService.skipThisUpdate(update)
            _uiState.update { it.copy(availableUpdate = null, isDownloading = false) }
        }
    }

    fun dismissForNow() {
        _uiState.update { it.copy(availableUpdate = null, isDownloading = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
