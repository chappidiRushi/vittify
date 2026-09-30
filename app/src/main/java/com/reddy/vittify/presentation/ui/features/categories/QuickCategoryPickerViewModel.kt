package com.reddy.vittify.presentation.ui.features.categories

import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.R
import com.reddy.vittify.data.database.dao.RecentSubcategoryInfo
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class QuickCategoryPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    subcategoryRepository: SubcategoryRepository
) : ViewModel() {

    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    val transactionId: Long = savedStateHandle.get<Long>(EXTRA_TRANSACTION_ID) ?: -1L
    val notificationId: Int = savedStateHandle.get<Int>(EXTRA_NOTIFICATION_ID) ?: -1

    private val _transaction = MutableStateFlow<TransactionEntity?>(null)
    val transaction: StateFlow<TransactionEntity?> = _transaction.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.categories

    val subcategoriesMap: StateFlow<Map<Long, List<SubcategoryEntity>>> =
        subcategoryRepository.subcategoriesMap

    private val recentlyUsedSubcategoriesFlow: Flow<List<RecentSubcategoryInfo>> =
        transactionRepository.getRecentlyUsedSubcategories(10)

    val recentSubcategories: StateFlow<List<Pair<CategoryEntity, SubcategoryEntity>>> =
        combine(
            categories,
            subcategoriesMap,
            recentlyUsedSubcategoriesFlow
        ) { catList, subMap, recentInfos ->
            val result = mutableListOf<Pair<CategoryEntity, SubcategoryEntity>>()
            for (info in recentInfos) {
                val cat = catList.find { it.name.equals(info.category, ignoreCase = true) }
                val sub = cat?.let { c ->
                    subMap[c.id]?.find { it.name.equals(info.subcategory, ignoreCase = true) }
                }
                if (cat != null && sub != null) {
                    result.add(Pair(cat, sub))
                }
            }
            result
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        if (transactionId != -1L) {
            viewModelScope.launch {
                _transaction.value = transactionRepository.getTransactionById(transactionId)
            }
        }
    }

    fun selectCategory(category: CategoryEntity, subcategory: SubcategoryEntity?) {
        if (transactionId == -1L) {
            _isCompleted.value = true
            return
        }
        viewModelScope.launch {
            val currentTx = _transaction.value ?: transactionRepository.getTransactionById(transactionId)
            if (currentTx != null) {
                val updated = currentTx.copy(
                    category = category.name,
                    subcategory = subcategory?.name ?: currentTx.subcategory,
                    updatedAt = LocalDateTime.now()
                )
                transactionRepository.updateTransaction(updated)
            }

            // Dismiss notification if ID provided
            if (notificationId != -1) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }

            val categoryLabel = if (subcategory != null) {
                "${category.name} • ${subcategory.name}"
            } else {
                category.name
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context.applicationContext,
                    context.getString(R.string.category_saved_format, categoryLabel),
                    Toast.LENGTH_SHORT
                ).show()
            }

            _isCompleted.value = true
        }
    }
}

