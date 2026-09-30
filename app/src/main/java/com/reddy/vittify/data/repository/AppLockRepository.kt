package com.reddy.vittify.data.repository

import com.reddy.vittify.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing app lock state and authentication.
 */
@Singleton
class AppLockRepository @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    /**
     * Check if app should be locked and update internal locked state.
     */
    suspend fun checkAndLock(): Boolean {
        val shouldLock = shouldLockApp()
        if (shouldLock) {
            _isLocked.value = true
        }
        return shouldLock
    }

    /**
     * Clear the locked state upon successful authentication.
     */
    fun unlock() {
        _isLocked.value = false
    }

    /**
     * Flow indicating if app lock is enabled
     */
    val isAppLockEnabled: Flow<Boolean> = userPreferencesRepository.isAppLockEnabled

    /**
     * Flow indicating timeout duration in minutes
     */
    val timeoutMinutes: Flow<Int> = userPreferencesRepository.appLockTimeoutMinutes

    /**
     * Enable or disable app lock
     * Uses atomic update to prevent race conditions
     */
    suspend fun setAppLockEnabled(enabled: Boolean) {
        // Use atomic update to set both enabled and timestamp together
        userPreferencesRepository.setAppLockEnabledWithTimestamp(enabled)
    }

    /**
     * Set the timeout duration in minutes
     * 0 = immediately, 1 = 1 minute, 5 = 5 minutes, etc.
     * Also updates auth timestamp to prevent immediate lock
     */
    suspend fun setTimeoutMinutes(minutes: Int) {
        // Use atomic update to set both timeout and timestamp together
        userPreferencesRepository.setAppLockTimeoutWithTimestamp(minutes)
    }

    /**
     * Update the last authentication timestamp to current time
     */
    suspend fun updateAuthTimestamp() {
        userPreferencesRepository.setLastAuthTimestamp(System.currentTimeMillis())
    }

    /**
     * Check if the app should be locked based on timeout
     * Returns true if app lock is enabled and timeout has elapsed
     */
    suspend fun shouldLockApp(): Boolean {
        val isEnabled = userPreferencesRepository.isAppLockEnabled.first()
        if (!isEnabled) return false

        val timeoutMinutes = userPreferencesRepository.getAppLockTimeoutMinutes()
        val lastAuthTimestamp = userPreferencesRepository.getLastAuthTimestamp()

        // If timeout is 0, lock immediately
        if (timeoutMinutes == 0) return true

        // If no authentication timestamp, lock the app
        if (lastAuthTimestamp == 0L) return true

        val currentTime = System.currentTimeMillis()
        val timeoutMillis = timeoutMinutes * 60 * 1000L
        val timeSinceAuth = currentTime - lastAuthTimestamp

        return timeSinceAuth >= timeoutMillis
    }

    /**
     * Flow that emits whether the app should be locked
     * Combines app lock enabled state with timeout calculation
     */
    fun shouldLockAppFlow(): Flow<Boolean> = combine(
        userPreferencesRepository.isAppLockEnabled,
        userPreferencesRepository.appLockTimeoutMinutes,
        userPreferencesRepository.getLastAuthTimestampFlow()
    ) { isEnabled, timeoutMinutes, lastAuthTimestamp ->
        if (!isEnabled) return@combine false

        // If timeout is 0, lock immediately
        if (timeoutMinutes == 0) return@combine true

        // If no authentication timestamp, lock the app
        if (lastAuthTimestamp == 0L) return@combine true

        val currentTime = System.currentTimeMillis()
        val timeoutMillis = timeoutMinutes * 60 * 1000L
        val timeSinceAuth = currentTime - lastAuthTimestamp

        timeSinceAuth >= timeoutMillis
    }

    /**
     * Get the configured timeout in minutes
     */
    suspend fun getTimeoutMinutes(): Int {
        return userPreferencesRepository.getAppLockTimeoutMinutes()
    }
}
