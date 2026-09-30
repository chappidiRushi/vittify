
package com.reddy.vittify.data.repository

import android.content.Context
import com.reddy.vittify.data.database.dao.SubcategoryDao
import com.reddy.vittify.data.database.dao.CategoryDao
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import dagger.hilt.android.qualifiers.ApplicationContext
import com.reddy.vittify.utils.IconResolutionUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubcategoryRepository @Inject constructor(
    private val subcategoryDao: SubcategoryDao,
    private val categoryDao: CategoryDao,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope,
    private val syncChangeTracker: dagger.Lazy<com.reddy.vittify.data.sync.SyncChangeTracker>? = null
) {
    val subcategoriesMap: StateFlow<Map<Long, List<SubcategoryEntity>>> = subcategoryDao.getAllSubcategories()
        .map { allSubs -> allSubs.groupBy { it.categoryId } }
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )
    fun getSubcategoriesByCategoryId(categoryId: Long): Flow<List<SubcategoryEntity>> {
        return subcategoryDao.getSubcategoriesByCategoryId(categoryId)
    }

    fun getAllSubcategories(): Flow<List<SubcategoryEntity>> {
        return subcategoryDao.getAllSubcategories()
    }

    suspend fun getSubcategoryByName(name: String): SubcategoryEntity? {
        return subcategoryDao.getSubcategoryByName(name)
    }

    suspend fun createSubcategory(
        categoryId: Long,
        name: String,
        iconResId: Int = 0,
        iconName: String = "",
        color: String = "#757575"
    ): Long {
        val subcategory = SubcategoryEntity(
            categoryId = categoryId,
            name = name,
            iconResId = iconResId,
            iconName = iconName,
            color = color,
            isSystem = false
        )
        return subcategoryDao.insertSubcategory(subcategory)
        val id = subcategoryDao.insertSubcategory(subcategory)
        val category = categoryDao.getCategoryById(categoryId)
        val categoryName = category?.name ?: ""
        syncChangeTracker?.get()?.trackSubcategory(subcategory.copy(id = id), categoryName, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_INSERT)
        return id
    }

    suspend fun updateSubcategory(subcategory: SubcategoryEntity) {
        subcategoryDao.updateSubcategory(
            subcategory.copy(updatedAt = LocalDateTime.now())
        )
        val updated = subcategory.copy(updatedAt = LocalDateTime.now())
        subcategoryDao.updateSubcategory(updated)
        val category = categoryDao.getCategoryById(updated.categoryId)
        val categoryName = category?.name ?: ""
        syncChangeTracker?.get()?.trackSubcategory(updated, categoryName, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
    }

    suspend fun resetSubcategoryToDefault(subcategoryId: Long) {
        val subcategory = subcategoryDao.getSubcategoryById(subcategoryId)
        if (subcategory != null && subcategory.isSystem) {
            // Reset to default values
            val resetSubcategory = subcategory.copy(
                name = subcategory.defaultName ?: subcategory.name,
                iconResId = subcategory.defaultIconResId ?: subcategory.iconResId,
                iconName = subcategory.defaultIconName ?: subcategory.iconName,
                color = subcategory.defaultColor ?: subcategory.color,
                updatedAt = LocalDateTime.now()
            )
            subcategoryDao.updateSubcategory(resetSubcategory)
            val category = categoryDao.getCategoryById(resetSubcategory.categoryId)
            val categoryName = category?.name ?: ""
            syncChangeTracker?.get()?.trackSubcategory(resetSubcategory, categoryName, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
        }
    }

    suspend fun deleteSubcategory(subcategory: SubcategoryEntity): Boolean {
        // Only delete non-system subcategories
        if (!subcategory.isSystem) {
            subcategoryDao.deleteSubcategory(subcategory)
            val category = categoryDao.getCategoryById(subcategory.categoryId)
            val categoryName = category?.name ?: ""
            syncChangeTracker?.get()?.trackSubcategory(subcategory, categoryName, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
            return true
        }
        return false
    }

    suspend fun deleteSubcategoryById(subcategoryId: Long): Boolean {
        val subcategory = subcategoryDao.getSubcategoryById(subcategoryId)
        if (subcategory != null && !subcategory.isSystem) {
            subcategoryDao.deleteSubcategoryById(subcategoryId)
            val category = categoryDao.getCategoryById(subcategory.categoryId)
            val categoryName = category?.name ?: ""
            syncChangeTracker?.get()?.trackSubcategory(subcategory, categoryName, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
            return true
        }
        return false
    }

    suspend fun initializeDefaultSubcategories() {
        if (subcategoryDao.getSubcategoryCount() == 0) {
            val foodCategory = categoryDao.getCategoryByName("Food & Drinks")
            if (foodCategory != null) {
                val defaultSubcategories = listOf(
                    "Eat out",
                    "Take Away",
                    "Tea & Coffee",
                    "FastFood",
                    "Snacks"
                ).map { name ->
                    SubcategoryEntity(
                        categoryId = foodCategory.id,
                        name = name
                    )
                }
                subcategoryDao.insertSubcategories(defaultSubcategories)
            }
        }
    }
}
