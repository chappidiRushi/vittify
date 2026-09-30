package com.reddy.vittify.presentation.ui.features.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.manager.NotificationScheduler
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationScheduler: NotificationScheduler,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val scanNewTransactionsEnabled: StateFlow<Boolean> = userPreferencesRepository.scanNewTransactionsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val scanNewTransactionsAlertTime: StateFlow<Long> = userPreferencesRepository.scanNewTransactionsAlertTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1200L) // 20:00

    val upcomingNotificationsEnabled: StateFlow<Boolean> = userPreferencesRepository.upcomingNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Combine active subscriptions with disabled notification IDs
    val subscriptions: StateFlow<List<SubscriptionNotificationState>> = combine(
        subscriptionRepository.getActiveSubscriptions(),
        userPreferencesRepository.disabledSubscriptionNotificationIds
    ) { subscriptions, disabledIds ->
        subscriptions.map { subscription ->
            SubscriptionNotificationState(
                subscription = subscription,
                isNotificationEnabled = !disabledIds.contains(subscription.id.toString())
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setScanNewTransactionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setScanNewTransactionsEnabled(enabled)
            notificationScheduler.scheduleDailyReminder()
        }
    }

    fun setScanNewTransactionsAlertTime(minutes: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setScanNewTransactionsAlertTime(minutes)
            notificationScheduler.scheduleDailyReminder()
        }
    }

    fun setUpcomingNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUpcomingNotificationsEnabled(enabled)
            notificationScheduler.scheduleDailyReminder()
        }
    }

    fun toggleSubscriptionNotification(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.toggleSubscriptionNotification(id, enabled)
        }
    }
}

data class SubscriptionNotificationState(
    val subscription: SubscriptionEntity,
    val isNotificationEnabled: Boolean
)
