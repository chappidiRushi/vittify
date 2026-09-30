package com.reddy.vittify.presentation.ui.features.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.CategoryType
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel
@Inject
constructor(
        private val categoryRepository: CategoryRepository,
        private val subcategoryRepository: SubcategoryRepository,
        private val transactionRepository: TransactionRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    // Tab Selection
    private val _selectedTab = MutableStateFlow(CategoryType.EXPENSE)
    val selectedTab: StateFlow<CategoryType> = _selectedTab.asStateFlow()

    fun selectTab(tab: CategoryType) {
        _selectedTab.value = tab
    }

    // Categories list
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.categories

    // Counts per category type
    val categoryCounts: StateFlow<Map<CategoryType, Int>> = categories.map { list ->
        CategoryType.entries.associateWith { type ->
            list.count { it.categoryType == type }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CategoryType.entries.associateWith { type ->
            categoryRepository.categories.value.count { it.categoryType == type }
        }
    )

    // Subcategories map (category ID -> list of subcategories)
    val subcategories: StateFlow<Map<Long, List<SubcategoryEntity>>> = 
        subcategoryRepository.subcategoriesMap

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Deletion Flow State
    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

    private val _categoryToDelete = MutableStateFlow<CategoryEntity?>(null)
    val categoryToDelete: StateFlow<CategoryEntity?> = _categoryToDelete.asStateFlow()

    private val _hasTransactions = MutableStateFlow(false)
    val hasTransactions: StateFlow<Boolean> = _hasTransactions.asStateFlow()

    private val _showMigrationSheet = MutableStateFlow(false)
    val showMigrationSheet: StateFlow<Boolean> = _showMigrationSheet.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Filtered categories
    val filteredCategories: StateFlow<List<CategoryEntity>> = combine(
        categories,
        subcategories,
        _searchQuery,
        _selectedTab
    ) { categories, subcategoriesMap, query, tab ->
        val byTab = categories.filter { it.categoryType == tab }
        if (query.isBlank()) {
            byTab
        } else {
            byTab.filter { category ->
                val categoryMatches = category.name.contains(query, ignoreCase = true)
                val subcategoriesMatch = subcategoriesMap[category.id]?.any {
                    it.name.contains(query, ignoreCase = true)
                } == true
                categoryMatches || subcategoriesMatch
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dialog states
    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()

    private val _editingCategory = MutableStateFlow<CategoryEntity?>(null)
    val editingCategory: StateFlow<CategoryEntity?> = _editingCategory.asStateFlow()

    private val _addCategoryInitialType = MutableStateFlow(CategoryType.EXPENSE)
    val addCategoryInitialType: StateFlow<CategoryType> = _addCategoryInitialType.asStateFlow()

    // Snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Subcategory Dialog states
    private val _showSubcategoryDialog = MutableStateFlow(false)
    val showSubcategoryDialog: StateFlow<Boolean> = _showSubcategoryDialog.asStateFlow()

    private val _editingSubcategory = MutableStateFlow<SubcategoryEntity?>(null)
    val editingSubcategory: StateFlow<SubcategoryEntity?> = _editingSubcategory.asStateFlow()

    private val _targetCategoryId = MutableStateFlow<Long?>(null)
    val targetCategoryId: StateFlow<Long?> = _targetCategoryId.asStateFlow()

    fun showAddDialog(type: CategoryType = _selectedTab.value) {
        _editingCategory.value = null
        _addCategoryInitialType.value = type
        _showAddEditDialog.value = true
    }

    fun showEditDialog(category: CategoryEntity) {
        // Allow editing system categories now (they can be edited but not deleted)
        _editingCategory.value = category
        _addCategoryInitialType.value = category.categoryType
        _showAddEditDialog.value = true
    }

    fun hideDialog() {
        _showAddEditDialog.value = false
        _editingCategory.value = null
    }

    fun showAddSubcategoryDialog(categoryId: Long) {
        _targetCategoryId.value = categoryId
        _editingSubcategory.value = null
        _showSubcategoryDialog.value = true
    }

    fun showEditSubcategoryDialog(subcategory: SubcategoryEntity) {
        _targetCategoryId.value = subcategory.categoryId
        _editingSubcategory.value = subcategory
        _showSubcategoryDialog.value = true
    }

    fun hideSubcategoryDialog() {
        _showSubcategoryDialog.value = false
        _editingSubcategory.value = null
        _targetCategoryId.value = null
    }

    fun saveCategory(
        name: String,
        description: String,
        color: String,
        iconResId: Int,
        iconName: String,
        categoryType: CategoryType
    ) {
        viewModelScope.launch {
            try {
                val editingCat = _editingCategory.value

                if (editingCat != null) {
                    // Update existing category
                    categoryRepository.updateCategory(
                            editingCat.copy(
                                name = name,
                                description = description,
                                color = color,
                                iconResId = iconResId,
                                iconName = iconName,
                                isIncome = categoryType == CategoryType.INCOME,
                                categoryType = categoryType
                            )
                    )
                    _snackbarMessage.value = "Category updated successfully"
                } else {
                    // Check if category already exists
                    if (categoryRepository.categoryExists(name)) {
                        _snackbarMessage.value = "Category '$name' already exists"
                        return@launch
                    }

                    // Create new category
                    categoryRepository.createCategory(
                        name = name,
                        description = description,
                        color = color,
                        iconResId = iconResId,
                        iconName = iconName,
                        categoryType = categoryType
                    )
                    _snackbarMessage.value = "Category created successfully"
                }

                hideDialog()
            } catch (e: Exception) {
                _snackbarMessage.value = "Error saving category: ${e.message}"
            }
        }
    }

    fun resetCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                categoryRepository.resetCategoryToDefault(categoryId)
                _snackbarMessage.value = "Category reset to default"
            } catch (e: Exception) {
                _snackbarMessage.value = "Error resetting category: ${e.message}"
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (category.isSystem) {
            _snackbarMessage.value = "System categories cannot be deleted"
            return
        }

        viewModelScope.launch {
            try {
                _categoryToDelete.value = category
                val count = transactionRepository.getTransactionCountByCategory(category.name)
                _hasTransactions.value = count > 0
                _showDeleteConfirmation.value = true
            } catch (e: Exception) {
                _snackbarMessage.value = "Error checking transactions: ${e.message}"
            }
        }
    }

    fun dismissDeleteConfirmation() {
        _showDeleteConfirmation.value = false
        _categoryToDelete.value = null
        _hasTransactions.value = false
    }

    fun showMigrationSheet() {
        _showDeleteConfirmation.value = false
        _showMigrationSheet.value = true
    }

    fun hideMigrationSheet() {
        _showMigrationSheet.value = false
        _categoryToDelete.value = null
        _hasTransactions.value = false
    }

    fun confirmDelete(moveToMiscellaneous: Boolean = true) {
        val category = _categoryToDelete.value ?: return
        
        viewModelScope.launch {
            try {
                if (moveToMiscellaneous && _hasTransactions.value) {
                    // Move transactions to "Miscellaneous"
                    transactionRepository.updateTransactionsCategory(
                        oldCategory = category.name,
                        newCategory = "Miscellaneous",
                        newSubcategory = null
                    )
                }
                performDelete(category)
            } catch (e: Exception) {
                _snackbarMessage.value = "Error during deletion: ${e.message}"
            }
        }
    }

    fun confirmMigrationToCategory(newCategory: CategoryEntity, newSubcategory: SubcategoryEntity?) {
        val oldCategory = _categoryToDelete.value ?: return
        
        viewModelScope.launch {
            try {
                transactionRepository.updateTransactionsCategory(
                    oldCategory = oldCategory.name,
                    newCategory = newCategory.name,
                    newSubcategory = newSubcategory?.name
                )
                performDelete(oldCategory)
                hideMigrationSheet()
            } catch (e: Exception) {
                _snackbarMessage.value = "Error during migration: ${e.message}"
                hideMigrationSheet()
            }
        }
    }

    private suspend fun performDelete(category: CategoryEntity) {
        val deleted = categoryRepository.deleteCategory(category.id)
        if (deleted) {
            _snackbarMessage.value = "Category deleted successfully"
        } else {
            _snackbarMessage.value = "Cannot delete this category"
        }
        _showDeleteConfirmation.value = false
        _categoryToDelete.value = null
        _hasTransactions.value = false
    }

    fun saveSubcategory(name: String, iconResId: Int, iconName: String, color: String) {
        val categoryId = _targetCategoryId.value ?: return
        val editingSub = _editingSubcategory.value

        viewModelScope.launch {
            try {
                if (editingSub != null) {
                    subcategoryRepository.updateSubcategory(
                        editingSub.copy(name = name, iconResId = iconResId, iconName = iconName, color = color)
                    )
                    _snackbarMessage.value = "Subcategory updated"
                } else {
                    subcategoryRepository.createSubcategory(
                        categoryId = categoryId,
                        name = name,
                        iconResId = iconResId,
                        iconName = iconName,
                        color = color
                    )
                    _snackbarMessage.value = "Subcategory added"
                }
                hideSubcategoryDialog()
            } catch (e: Exception) {
                _snackbarMessage.value = "Error saving subcategory: ${e.message}"
            }
        }
    }

    fun resetSubcategory(subcategoryId: Long) {
        viewModelScope.launch {
            try {
                subcategoryRepository.resetSubcategoryToDefault(subcategoryId)
                _snackbarMessage.value = "Subcategory reset to default"
            } catch (e: Exception) {
                _snackbarMessage.value = "Error resetting subcategory: ${e.message}"
            }
        }
    }

    fun deleteSubcategory(subcategory: SubcategoryEntity) {
        if (subcategory.isSystem) {
            _snackbarMessage.value = "System subcategories cannot be deleted"
            return
        }

        viewModelScope.launch {
            try {
                val deleted = subcategoryRepository.deleteSubcategory(subcategory)
                if (deleted) {
                    _snackbarMessage.value = "Subcategory deleted"
                } else {
                    _snackbarMessage.value = "Cannot delete this subcategory"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Error deleting subcategory: ${e.message}"
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}

data class CategoriesUiState(val isLoading: Boolean = false, val errorMessage: String? = null)
