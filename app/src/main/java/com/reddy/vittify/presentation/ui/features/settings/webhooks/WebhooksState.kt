package com.reddy.vittify.presentation.ui.features.settings.webhooks

import com.reddy.vittify.data.database.entity.WebhookLogEntity
import com.reddy.vittify.data.database.entity.WebhookProfileEntity
import com.reddy.vittify.data.webhook.WebhookSettings

data class WebhooksUiState(
    val profiles: List<WebhookProfileEntity> = emptyList(),
    val logs: List<WebhookLogEntity> = emptyList(),
    val settings: WebhookSettings = WebhookSettings(),
    val settingsLoaded: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null
)
