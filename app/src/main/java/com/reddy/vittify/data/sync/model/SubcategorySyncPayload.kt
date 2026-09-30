package com.reddy.vittify.data.sync.model

import androidx.annotation.Keep

@Keep
data class SubcategorySyncPayload(
    val categoryName: String,
    val name: String,
    val iconResId: Int = 0,
    val iconName: String = "",
    val color: String = "#757575",
    val isSystem: Boolean = false,
    val defaultName: String? = null,
    val defaultIconResId: Int? = null,
    val defaultIconName: String? = null,
    val defaultColor: String? = null
)

