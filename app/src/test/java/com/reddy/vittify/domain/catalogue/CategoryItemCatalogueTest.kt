package com.reddy.vittify.domain.catalogue

import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.CategoryType
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryItemCatalogueTest {

    private val groceriesCategory = CategoryEntity(
        id = 1L,
        name = "Groceries",
        color = "#9E7155",
        iconResId = 0,
        iconName = "",
        description = "Kitchen supplies",
        isIncome = false,
        categoryType = CategoryType.EXPENSE,
        displayOrder = 1
    )

    private val transportCategory = CategoryEntity(
        id = 2L,
        name = "Transport",
        color = "#0066CC",
        iconResId = 0,
        iconName = "",
        description = "Transport",
        isIncome = false,
        categoryType = CategoryType.EXPENSE,
        displayOrder = 2
    )

    private val medicalCategory = CategoryEntity(
        id = 3L,
        name = "Medical",
        color = "#FF0041",
        iconResId = 0,
        iconName = "",
        description = "Medical",
        isIncome = false,
        categoryType = CategoryType.EXPENSE,
        displayOrder = 3
    )

    private val dairySubcategory = SubcategoryEntity(
        id = 101L,
        categoryId = 1L,
        name = "Dairy",
        iconResId = 0,
        iconName = "",
        color = "#FF0041"
    )

    private val vegetablesSubcategory = SubcategoryEntity(
        id = 102L,
        categoryId = 1L,
        name = "Vegetables",
        iconResId = 0,
        iconName = "",
        color = "#423D3A"
    )

    private val fruitsSubcategory = SubcategoryEntity(
        id = 103L,
        categoryId = 1L,
        name = "Fruits",
        iconResId = 0,
        iconName = "",
        color = "#B75300"
    )

    private val fuelSubcategory = SubcategoryEntity(
        id = 201L,
        categoryId = 2L,
        name = "Fuel",
        iconResId = 0,
        iconName = "",
        color = "#FF9800"
    )

    private val medicinesSubcategory = SubcategoryEntity(
        id = 301L,
        categoryId = 3L,
        name = "Medicines",
        iconResId = 0,
        iconName = "",
        color = "#423D3A"
    )

    private val categories = listOf(groceriesCategory, transportCategory, medicalCategory)
    private val subcategoriesMap = mapOf(
        1L to listOf(dairySubcategory, vegetablesSubcategory, fruitsSubcategory),
        2L to listOf(fuelSubcategory),
        3L to listOf(medicinesSubcategory)
    )

    @Test
    fun `searching milk suggests Dairy under Groceries`() {
        val matches = CategoryItemCatalogue.searchSuggestions(
            categories = categories,
            subcategoriesMap = subcategoriesMap,
            query = "milk"
        )

        assertTrue("Expected non-empty suggestions for 'milk'", matches.isNotEmpty())
        val topMatch = matches.first()
        assertEquals("Groceries", topMatch.category.name)
        assertEquals("Dairy", topMatch.subcategory?.name)
        assertNotNull(topMatch.matchedItem)
        assertTrue(topMatch.matchedItem!!.contains("milk", ignoreCase = true))
    }

    @Test
    fun `searching carrot suggests Vegetables under Groceries`() {
        val matches = CategoryItemCatalogue.searchSuggestions(
            categories = categories,
            subcategoriesMap = subcategoriesMap,
            query = "carrot"
        )

        assertTrue("Expected non-empty suggestions for 'carrot'", matches.isNotEmpty())
        val topMatch = matches.first()
        assertEquals("Groceries", topMatch.category.name)
        assertEquals("Vegetables", topMatch.subcategory?.name)
        assertNotNull(topMatch.matchedItem)
        assertTrue(topMatch.matchedItem!!.contains("carrot", ignoreCase = true))
    }

    @Test
    fun `searching petrol suggests Fuel under Transport`() {
        val matches = CategoryItemCatalogue.searchSuggestions(
            categories = categories,
            subcategoriesMap = subcategoriesMap,
            query = "petrol"
        )

        assertTrue(matches.isNotEmpty())
        val topMatch = matches.first()
        assertEquals("Transport", topMatch.category.name)
        assertEquals("Fuel", topMatch.subcategory?.name)
    }

    @Test
    fun `searching paracetamol suggests Medicines under Medical`() {
        val matches = CategoryItemCatalogue.searchSuggestions(
            categories = categories,
            subcategoriesMap = subcategoriesMap,
            query = "paracetamol"
        )

        assertTrue(matches.isNotEmpty())
        val topMatch = matches.first()
        assertEquals("Medical", topMatch.category.name)
        assertEquals("Medicines", topMatch.subcategory?.name)
    }

    @Test
    fun `matchesSubcategory returns true for catalogue items`() {
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Groceries", "Dairy", "milk"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Groceries", "Dairy", "curd"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Groceries", "Dairy", "paneer"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Groceries", "Dairy", "doodh"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Groceries", "Vegetables", "carrot"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Groceries", "Vegetables", "potato"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Groceries", "Vegetables", "tomato"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Groceries", "Fruits", "apple"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Groceries", "Fruits", "banana"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Transport", "Fuel", "petrol"))
        assertTrue(CategoryItemCatalogue.matchesSubcategory("Transport", "Fuel", "diesel"))
    }
}

