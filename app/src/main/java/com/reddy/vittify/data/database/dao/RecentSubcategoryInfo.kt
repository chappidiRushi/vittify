package com.reddy.vittify.data.database.dao

import androidx.room.ColumnInfo

data class RecentSubcategoryInfo(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "subcategory") val subcategory: String
)
