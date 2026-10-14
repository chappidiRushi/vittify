package com.reddy.vittify.data.backup.importwizard.templates

import android.content.Context
import android.net.Uri
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelTemplateGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            "Food & Drinks", "Transport", "Shopping", "Groceries", "Home",
            "Entertainment", "Events", "Travel", "Medical", "Personal",
            "Education", "Utilities", "Investment", "Income", "Other"
        )
        val DEFAULT_SUBCATEGORIES = mapOf(
            "Food & Drinks" to listOf("Eating out", "Take Away", "Tea & Coffee", "Fast Food", "Snacks", "Beverages"),
            "Transport" to listOf("Fuel", "Taxi & Cab", "Bus", "Train", "Metro", "Flight"),
            "Shopping" to listOf("Clothes", "Electronics", "Groceries", "Footwear", "Accessories"),
            "Groceries" to listOf("Vegetables", "Fruits", "Dairy", "Meat", "Pantry"),
            "Entertainment" to listOf("Movies", "Streaming", "Games", "Events"),
            "Utilities" to listOf("Electricity", "Water", "Gas", "Internet", "Mobile Recharge"),
            "Medical" to listOf("Medicines", "Doctor Consultation", "Lab Tests"),
            "Personal" to listOf("Salon & Spa", "Fitness & Gym", "Cosmetics"),
            "Travel" to listOf("Hotel", "Flights", "Sightseeing"),
            "Education" to listOf("Courses", "Books", "Tuition"),
            "Investment" to listOf("Mutual Funds", "Stocks", "Fixed Deposit", "Gold"),
            "Income" to listOf("Salary", "Freelance", "Bonus", "Dividends")
        )
        val DEFAULT_TRANSACTION_TYPES = listOf(
            "EXPENSE", "INCOME", "TRANSFER", "CREDIT", "INVESTMENT", "BALANCE_UPDATE"
        )
        val DEFAULT_ACCOUNT_TYPES = listOf(
            "SAVINGS", "CURRENT", "CREDIT_CARD", "WALLET"
        )
        val DEFAULT_BILLING_CYCLES = listOf(
            "MONTHLY", "YEARLY", "WEEKLY", "DAILY", "QUARTERLY"
        )
        val DEFAULT_CATEGORY_TYPES = listOf(
            "EXPENSE", "INCOME"
        )

        fun toExcelRangeName(name: String): String {
            return name.replace("&", "_").replace(" ", "_").replace("-", "_")
        }
    }

    suspend fun generateTemplateFile(): File = withContext(Dispatchers.IO) {
        val appCategories: List<String> = try {
            val dbCategories = categoryRepository.getAllCategories().first()
            if (dbCategories.isNotEmpty()) dbCategories.map { it.name }.distinct() else DEFAULT_CATEGORIES
        } catch (e: Exception) {
            DEFAULT_CATEGORIES
        }

        val appSubcategoriesMap: Map<String, List<String>> = try {
            val dbCats = categoryRepository.getAllCategories().first()
            val dbSubs = subcategoryRepository.getAllSubcategories().first()
            val catIdToName = dbCats.associate { it.id to it.name }
            val map = mutableMapOf<String, MutableList<String>>()
            dbSubs.forEach { sub ->
                val parentName = catIdToName[sub.categoryId]
                if (parentName != null) {
                    map.getOrPut(parentName) { mutableListOf() }.add(sub.name)
                }
            }
            if (map.isNotEmpty()) map else DEFAULT_SUBCATEGORIES
        } catch (e: Exception) {
            DEFAULT_SUBCATEGORIES
        }

        val workbook = XSSFWorkbook()
        val templateFile = File(context.cacheDir, "vittify_sample_template.xlsx")

        try {
            // Header cell styles
            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            val headerStyle = (workbook.createCellStyle() as org.apache.poi.xssf.usermodel.XSSFCellStyle).apply {
                setFont(headerFont)
                fillForegroundColor = IndexedColors.ROYAL_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
            }

            val titleFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 14
                color = IndexedColors.ROYAL_BLUE.index
            }
            val titleStyle = (workbook.createCellStyle() as org.apache.poi.xssf.usermodel.XSSFCellStyle).apply {
                setFont(titleFont)
            }

            // 1. Instructions Sheet
            val instructionsSheet = workbook.createSheet("Instructions")
            createInstructionsSheet(instructionsSheet, titleStyle, headerStyle)

            // 2. Lookup Sheet (hidden, provides data validation ranges & named ranges for subcategories)
            val lookupSheet = workbook.createSheet("_Lookup")
            createLookupSheet(lookupSheet, workbook, appCategories, appSubcategoriesMap)
            val lookupIndex = workbook.getSheetIndex("_Lookup")
            workbook.setSheetHidden(lookupIndex, true)

            // 3. Bank Accounts Sheet
            val accountsSheet = workbook.createSheet("Bank Accounts") as XSSFSheet
            createBankAccountsSheet(accountsSheet, headerStyle)

            // 4. Categories Sheet
            val categoriesSheet = workbook.createSheet("Categories") as XSSFSheet
            createCategoriesSheet(categoriesSheet, headerStyle)

            // 5. Subcategories Sheet
            val subcategoriesSheet = workbook.createSheet("Subcategories") as XSSFSheet
            createSubcategoriesSheet(subcategoriesSheet, headerStyle, appCategories.size)

            // 6. Transactions Sheet (with Category & Dependent Subcategory Dropdowns)
            val transactionsSheet = workbook.createSheet("Transactions") as XSSFSheet
            createTransactionsSheet(transactionsSheet, headerStyle, appCategories.size)

            // 7. Subscriptions Sheet
            val subscriptionsSheet = workbook.createSheet("Subscriptions") as XSSFSheet
            createSubscriptionsSheet(subscriptionsSheet, headerStyle)

            // Auto-size columns on all visible sheets
            for (i in 0 until workbook.numberOfSheets) {
                if (i != lookupIndex) {
                    val sheet = workbook.getSheetAt(i)
                    for (col in 0..10) {
                        sheet.setColumnWidth(col, 22 * 256)
                    }
                }
            }

            FileOutputStream(templateFile).use { out ->
                workbook.write(out)
            }

            templateFile
        } finally {
            workbook.close()
        }
    }

    suspend fun saveTemplateToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val file = generateTemplateFile()
        context.contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input ->
                input.copyTo(output)
            }
            true
        } ?: false
    }

    private fun createInstructionsSheet(
        sheet: Sheet,
        titleStyle: org.apache.poi.xssf.usermodel.XSSFCellStyle,
        headerStyle: org.apache.poi.xssf.usermodel.XSSFCellStyle
    ) {
        var r = 0

        val row0 = sheet.createRow(r++)
        val c0 = row0.createCell(0)
        c0.setCellValue("Vittify - Data Import Template Instructions")
        c0.cellStyle = titleStyle

        r++ // blank row

        val guidelines = listOf(
            "Welcome to the Vittify Excel Import Template!",
            "",
            "1. Sheet: 'Bank Accounts'",
            "   - Enter each bank account, credit card, or cash wallet.",
            "   - 'Account Type' has a dropdown menu (SAVINGS, CURRENT, CREDIT_CARD, WALLET).",
            "   - 'Bank Name' is the main name (e.g. HDFC, SBI, Chase).",
            "",
            "2. Sheet: 'Categories' (Optional)",
            "   - Define any custom root categories you want to create.",
            "   - 'Type' dropdown allows EXPENSE or INCOME.",
            "",
            "3. Sheet: 'Subcategories' (Optional)",
            "   - Define custom subcategories linked to a parent category.",
            "",
            "4. Sheet: 'Transactions'",
            "   - Enter your income, expenses, and transfers.",
            "   - 'Category' has a dropdown with pre-populated in-app categories.",
            "   - 'Subcategory' automatically populates based on the selected Category!",
            "   - 'Type' has a dropdown menu (EXPENSE, INCOME, TRANSFER, CREDIT, INVESTMENT).",
            "   - 'Bank' matches the Bank Name from the 'Bank Accounts' sheet.",
            "",
            "5. Sheet: 'Subscriptions'",
            "   - Enter recurring payments like Netflix, Spotify, gym, rent.",
            "   - 'Billing Cycle' has a dropdown menu (MONTHLY, YEARLY, WEEKLY, DAILY, QUARTERLY).",
            "",
            "6. Tips for seamless import:",
            "   - Simply fill in the rows and import this file directly into Vittify.",
            "   - The Vittify Import Wizard lets you preview, customize, and map everything before saving!"
        )

        guidelines.forEach { line ->
            val row = sheet.createRow(r++)
            row.createCell(0).setCellValue(line)
        }
    }

    private fun createLookupSheet(
        sheet: Sheet,
        workbook: XSSFWorkbook,
        categories: List<String>,
        subcategoriesMap: Map<String, List<String>>
    ) {
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Categories")
        header.createCell(1).setCellValue("AccountTypes")
        header.createCell(2).setCellValue("TransactionTypes")
        header.createCell(3).setCellValue("BillingCycles")
        header.createCell(4).setCellValue("CategoryTypes")

        val maxRows = maxOf(
            categories.size,
            DEFAULT_ACCOUNT_TYPES.size,
            DEFAULT_TRANSACTION_TYPES.size,
            DEFAULT_BILLING_CYCLES.size,
            DEFAULT_CATEGORY_TYPES.size
        )

        for (i in 0 until maxRows) {
            val row = sheet.createRow(i + 1)
            if (i < categories.size) row.createCell(0).setCellValue(categories[i])
            if (i < DEFAULT_ACCOUNT_TYPES.size) row.createCell(1).setCellValue(DEFAULT_ACCOUNT_TYPES[i])
            if (i < DEFAULT_TRANSACTION_TYPES.size) row.createCell(2).setCellValue(DEFAULT_TRANSACTION_TYPES[i])
            if (i < DEFAULT_BILLING_CYCLES.size) row.createCell(3).setCellValue(DEFAULT_BILLING_CYCLES[i])
            if (i < DEFAULT_CATEGORY_TYPES.size) row.createCell(4).setCellValue(DEFAULT_CATEGORY_TYPES[i])
        }

        // Columns 5+ (F+) store subcategories for each category and create named ranges
        var colIndex = 5
        categories.forEach { catName ->
            val subs = subcategoriesMap[catName] ?: DEFAULT_SUBCATEGORIES[catName] ?: listOf("General")
            header.createCell(colIndex).setCellValue(catName)

            subs.forEachIndexed { sIdx, subName ->
                var row = sheet.getRow(sIdx + 1)
                if (row == null) row = sheet.createRow(sIdx + 1)
                row.createCell(colIndex).setCellValue(subName)
            }

            // Create Named Range in Workbook for dependent dropdowns
            try {
                val safeRangeName = toExcelRangeName(catName)
                val namedRange = workbook.createName()
                namedRange.nameName = safeRangeName
                val colLetter = CellReference.convertNumToColString(colIndex)
                val endRow = subs.size + 1
                namedRange.refersToFormula = "'_Lookup'!$${colLetter}$2:$${colLetter}$$endRow"
            } catch (e: Exception) {
                // Ignore if duplicate name
            }

            colIndex++
        }
    }

    private fun createBankAccountsSheet(sheet: XSSFSheet, headerStyle: org.apache.poi.xssf.usermodel.XSSFCellStyle) {
        val headers = listOf("Bank Name", "Account Name / Last 4", "Balance", "Currency", "Account Type")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        // Sample Rows
        val samples = listOf(
            listOf("HDFC Bank", "4321", "25000.00", "INR", "SAVINGS"),
            listOf("State Bank of India", "Salary", "82400.50", "INR", "SAVINGS"),
            listOf("ICICI Bank", "Coral CC", "0.00", "INR", "CREDIT_CARD"),
            listOf("Cash Wallet", "Pocket Cash", "1500.00", "INR", "WALLET")
        )

        samples.forEachIndexed { rIdx, rowData ->
            val row = sheet.createRow(rIdx + 1)
            rowData.forEachIndexed { cIdx, value ->
                row.createCell(cIdx).setCellValue(value)
            }
        }

        // Dropdown Data Validation for Account Type (Column E, index 4)
        val dvHelper = XSSFDataValidationHelper(sheet)
        val constraint = dvHelper.createFormulaListConstraint("'_Lookup'!\$B\$2:\$B\$${DEFAULT_ACCOUNT_TYPES.size + 1}")
        val addressList = CellRangeAddressList(1, 1000, 4, 4)
        val validation = dvHelper.createValidation(constraint, addressList)
        validation.showErrorBox = true
        validation.showPromptBox = true
        sheet.addValidationData(validation)
    }

    private fun createCategoriesSheet(sheet: XSSFSheet, headerStyle: org.apache.poi.xssf.usermodel.XSSFCellStyle) {
        val headers = listOf("Category Name", "Type", "Color")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        val samples = listOf(
            listOf("Food & Drinks", "EXPENSE", "#FC8019"),
            listOf("Transport", "EXPENSE", "#0066CC"),
            listOf("Shopping", "EXPENSE", "#893BBE"),
            listOf("Income", "INCOME", "#4CAF50")
        )

        samples.forEachIndexed { rIdx, rowData ->
            val row = sheet.createRow(rIdx + 1)
            rowData.forEachIndexed { cIdx, value ->
                row.createCell(cIdx).setCellValue(value)
            }
        }

        val dvHelper = XSSFDataValidationHelper(sheet)
        val typeConstraint = dvHelper.createFormulaListConstraint("'_Lookup'!\$E\$2:\$E\$${DEFAULT_CATEGORY_TYPES.size + 1}")
        val typeAddressList = CellRangeAddressList(1, 1000, 1, 1)
        val typeValidation = dvHelper.createValidation(typeConstraint, typeAddressList)
        typeValidation.showErrorBox = true
        typeValidation.showPromptBox = true
        sheet.addValidationData(typeValidation)
    }

    private fun createSubcategoriesSheet(sheet: XSSFSheet, headerStyle: org.apache.poi.xssf.usermodel.XSSFCellStyle, categoryCount: Int) {
        val headers = listOf("Category", "Subcategory Name")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        val samples = listOf(
            listOf("Food & Drinks", "Dining Out"),
            listOf("Food & Drinks", "Coffee & Tea"),
            listOf("Transport", "Taxi & Cab"),
            listOf("Shopping", "Clothes"),
            listOf("Income", "Salary")
        )

        samples.forEachIndexed { rIdx, rowData ->
            val row = sheet.createRow(rIdx + 1)
            rowData.forEachIndexed { cIdx, value ->
                row.createCell(cIdx).setCellValue(value)
            }
        }

        val dvHelper = XSSFDataValidationHelper(sheet)
        val catConstraint = dvHelper.createFormulaListConstraint("'_Lookup'!\$A\$2:\$A\$${categoryCount + 1}")
        val catAddressList = CellRangeAddressList(1, 1000, 0, 0)
        val catValidation = dvHelper.createValidation(catConstraint, catAddressList)
        catValidation.showErrorBox = false
        catValidation.showPromptBox = true
        sheet.addValidationData(catValidation)
    }

    private fun createTransactionsSheet(sheet: XSSFSheet, headerStyle: org.apache.poi.xssf.usermodel.XSSFCellStyle, categoryCount: Int) {
        val headers = listOf("Date", "Time", "Merchant", "Category", "Subcategory", "Type", "Amount", "Bank", "Account", "Note")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        val samples = listOf(
            listOf("2026-10-01", "09:30:00", "Starbucks Coffee", "Food & Drinks", "Tea & Coffee", "EXPENSE", "350.00", "HDFC Bank", "4321", "Morning mocha"),
            listOf("2026-10-02", "14:15:00", "TechCorp Ltd", "Income", "Salary", "INCOME", "75000.00", "State Bank of India", "Salary", "Monthly pay"),
            listOf("2026-10-03", "19:45:00", "Amazon.in", "Shopping", "Electronics", "EXPENSE", "1499.00", "ICICI Bank", "Coral CC", "USB Cable & Adapter"),
            listOf("2026-10-04", "11:00:00", "Self Transfer", "Other", "General", "TRANSFER", "5000.00", "State Bank of India", "Salary", "Transfer to HDFC")
        )

        samples.forEachIndexed { rIdx, rowData ->
            val row = sheet.createRow(rIdx + 1)
            rowData.forEachIndexed { cIdx, value ->
                row.createCell(cIdx).setCellValue(value)
            }
        }

        val dvHelper = XSSFDataValidationHelper(sheet)

        // Dropdown for Category (Column D, index 3)
        val catConstraint = dvHelper.createFormulaListConstraint("'_Lookup'!\$A\$2:\$A\$${categoryCount + 1}")
        val catAddressList = CellRangeAddressList(1, 1000, 3, 3)
        val catValidation = dvHelper.createValidation(catConstraint, catAddressList)
        catValidation.showErrorBox = false
        catValidation.showPromptBox = true
        sheet.addValidationData(catValidation)

        // Dependent Dropdown for Subcategory (Column E, index 4) populated dynamically based on Category cell D2
        val subcatConstraint = dvHelper.createFormulaListConstraint("INDIRECT(SUBSTITUTE(SUBSTITUTE(D2, \"&\", \"_\"), \" \", \"_\"))")
        val subcatAddressList = CellRangeAddressList(1, 1000, 4, 4)
        val subcatValidation = dvHelper.createValidation(subcatConstraint, subcatAddressList)
        subcatValidation.showErrorBox = false
        subcatValidation.showPromptBox = true
        sheet.addValidationData(subcatValidation)

        // Dropdown for Type (Column F, index 5)
        val typeConstraint = dvHelper.createFormulaListConstraint("'_Lookup'!\$C\$2:\$C\$${DEFAULT_TRANSACTION_TYPES.size + 1}")
        val typeAddressList = CellRangeAddressList(1, 1000, 5, 5)
        val typeValidation = dvHelper.createValidation(typeConstraint, typeAddressList)
        typeValidation.showErrorBox = true
        typeValidation.showPromptBox = true
        sheet.addValidationData(typeValidation)
    }

    private fun createSubscriptionsSheet(sheet: XSSFSheet, headerStyle: org.apache.poi.xssf.usermodel.XSSFCellStyle) {
        val headers = listOf("Merchant", "Amount", "Billing Cycle", "Next Payment Date", "Category", "Bank")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        // Sample Rows
        val samples = listOf(
            listOf("Netflix", "649.00", "MONTHLY", "2026-11-01", "Entertainment", "ICICI Bank"),
            listOf("Spotify Premium", "119.00", "MONTHLY", "2026-11-05", "Entertainment", "HDFC Bank"),
            listOf("Google One 2TB", "6500.00", "YEARLY", "2027-04-15", "Utilities", "HDFC Bank")
        )

        samples.forEachIndexed { rIdx, rowData ->
            val row = sheet.createRow(rIdx + 1)
            rowData.forEachIndexed { cIdx, value ->
                row.createCell(cIdx).setCellValue(value)
            }
        }

        // Dropdown for Billing Cycle (Column C, index 2)
        val dvHelper = XSSFDataValidationHelper(sheet)
        val cycleConstraint = dvHelper.createFormulaListConstraint("'_Lookup'!\$D\$2:\$D\$${DEFAULT_BILLING_CYCLES.size + 1}")
        val cycleAddressList = CellRangeAddressList(1, 1000, 2, 2)
        val cycleValidation = dvHelper.createValidation(cycleConstraint, cycleAddressList)
        cycleValidation.showErrorBox = true
        cycleValidation.showPromptBox = true
        sheet.addValidationData(cycleValidation)
    }
}
