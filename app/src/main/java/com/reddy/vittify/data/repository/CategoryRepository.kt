package com.reddy.vittify.data.repository

import android.content.Context
import com.reddy.vittify.data.database.dao.CategoryDao
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.qualifiers.ApplicationContext
import com.reddy.vittify.utils.IconResolutionUtils
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope,
    private val syncChangeTracker: dagger.Lazy<com.reddy.vittify.data.sync.SyncChangeTracker>? = null
) {
    
    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
    
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }
    
    fun getCategoriesByType(categoryType: com.reddy.vittify.data.database.entity.CategoryType): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(categoryType)
    }
    
    fun getExpenseCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getExpenseCategories()
    }
    
    fun getIncomeCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getIncomeCategories()
    }
    
    suspend fun getCategoryById(categoryId: Long): CategoryEntity? {
        return categoryDao.getCategoryById(categoryId)
    }
    
    suspend fun getCategoryByName(categoryName: String): CategoryEntity? {
        return categoryDao.getCategoryByName(categoryName)
    }
    
    suspend fun createCategory(
        name: String,
        description: String = "",
        color: String,
        iconResId: Int = 0,
        iconName: String = "",
        categoryType: com.reddy.vittify.data.database.entity.CategoryType = com.reddy.vittify.data.database.entity.CategoryType.EXPENSE
    ): Long {
        val category = CategoryEntity(
            name = name,
            description = description,
            color = color,
            iconResId = iconResId,
            iconName = iconName,
            isSystem = false,
            isIncome = categoryType == com.reddy.vittify.data.database.entity.CategoryType.INCOME,
            categoryType = categoryType,
            displayOrder = 999
        )
        val id = categoryDao.insertCategory(category)
        syncChangeTracker?.get()?.trackCategory(category.copy(id = id), com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_INSERT)
        return id
    }
    
    suspend fun resetCategoryToDefault(categoryId: Long) {
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null && category.isSystem) {
            // Reset to default values
            val defaultType = category.defaultCategoryType?.let { com.reddy.vittify.data.database.entity.CategoryType.fromString(it) } ?: category.categoryType
            val resetCategory = category.copy(
                name = category.defaultName ?: category.name,
                description = category.defaultDescription ?: category.description,
                color = category.defaultColor ?: category.color,
                iconResId = category.defaultIconResId ?: category.iconResId,
                iconName = category.defaultIconName ?: category.iconName,
                categoryType = defaultType,
                isIncome = defaultType == com.reddy.vittify.data.database.entity.CategoryType.INCOME,
                updatedAt = LocalDateTime.now()
            )
            categoryDao.updateCategory(resetCategory)
            syncChangeTracker?.get()?.trackCategory(resetCategory, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
        }
    }
    
    suspend fun updateCategory(category: CategoryEntity) {
        val updated = category.copy(updatedAt = LocalDateTime.now())
        categoryDao.updateCategory(updated)
        syncChangeTracker?.get()?.trackCategory(updated, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
    }
    
    suspend fun deleteCategory(categoryId: Long): Boolean {
        // Only delete non-system categories
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null && !category.isSystem) {
            categoryDao.deleteCategory(categoryId)
            syncChangeTracker?.get()?.trackCategory(category, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
            return true
        }
        return false
    }
    
    suspend fun categoryExists(categoryName: String): Boolean {
        return categoryDao.categoryExists(categoryName)
    }
}