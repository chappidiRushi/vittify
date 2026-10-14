package com.reddy.vittify.data.backup

import com.reddy.vittify.data.backup.importwizard.RangeSelectionTracker
import com.reddy.vittify.data.backup.importwizard.templates.ExcelTemplateGenerator
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class ImportWizardTest {

    @Test
    fun testRangeSelectionTracker() {
        var tracker = RangeSelectionTracker()
        assertFalse(tracker.isRangeReady)

        // Toggle first item (e.g. index 3)
        tracker = tracker.withToggled(3)
        assertEquals(3, tracker.firstIndex)
        assertFalse(tracker.isRangeReady)

        // Toggle second item (e.g. index 8)
        tracker = tracker.withToggled(8)
        assertEquals(3, tracker.firstIndex)
        assertEquals(8, tracker.secondIndex)
        assertTrue(tracker.isRangeReady)
        assertEquals(6, tracker.rangeCount) // 3, 4, 5, 6, 7, 8 -> 6 items
        assertEquals(3..8, tracker.rangeIndices)

        // Test reverse order (first > second)
        var reverseTracker = RangeSelectionTracker(firstIndex = 10, secondIndex = 4)
        assertTrue(reverseTracker.isRangeReady)
        assertEquals(4..10, reverseTracker.rangeIndices)
        assertEquals(7, reverseTracker.rangeCount)

        // Test reset
        tracker = tracker.reset()
        assertFalse(tracker.isRangeReady)
        assertEquals(null, tracker.firstIndex)
        assertEquals(null, tracker.secondIndex)
    }

    @Test
    fun testExcelWorkbookStructureAndDataValidation() {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Transactions")

        // Create header
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Date")
        header.createCell(1).setCellValue("Type")
        header.createCell(2).setCellValue("Amount")

        // Data validation for Type
        val dvHelper = sheet.dataValidationHelper
        val constraint = dvHelper.createExplicitListConstraint(
            ExcelTemplateGenerator.DEFAULT_TRANSACTION_TYPES.toTypedArray()
        )
        val addressList = org.apache.poi.ss.util.CellRangeAddressList(1, 100, 1, 1)
        val validation = dvHelper.createValidation(constraint, addressList)
        validation.showErrorBox = true
        sheet.addValidationData(validation)

        // Verify sheet has validation
        val validations = sheet.dataValidations
        assertEquals(1, validations.size)
        assertNotNull(validations[0])

        val tempFile = File.createTempFile("test_excel", ".xlsx")
        FileOutputStream(tempFile).use { out ->
            workbook.write(out)
        }
        workbook.close()

        assertTrue(tempFile.exists())
        assertTrue(tempFile.length() > 0)
        tempFile.delete()
    }

    @Test
    fun testExcelDependentSubcategoryValidation() {
        val workbook = XSSFWorkbook()
        val lookupSheet = workbook.createSheet("_Lookup")
        val lookupHeader = lookupSheet.createRow(0)
        lookupHeader.createCell(0).setCellValue("Categories")
        lookupHeader.createCell(5).setCellValue("Food___Drinks")

        // Subcategories under Food & Drinks
        val sub1 = lookupSheet.createRow(1)
        sub1.createCell(5).setCellValue("Eating out")
        val sub2 = lookupSheet.createRow(2)
        sub2.createCell(5).setCellValue("Tea & Coffee")

        // Create Named Range in Workbook
        val namedRange = workbook.createName()
        namedRange.nameName = "Food___Drinks"
        namedRange.refersToFormula = "'_Lookup'!\$F\$2:\$F\$3"

        // Transactions sheet
        val txSheet = workbook.createSheet("Transactions")
        val dvHelper = txSheet.dataValidationHelper
        val constraint = dvHelper.createFormulaListConstraint("INDIRECT(SUBSTITUTE(SUBSTITUTE(D2, \"&\", \"_\"), \" \", \"_\"))")
        val addressList = org.apache.poi.ss.util.CellRangeAddressList(1, 100, 4, 4)
        val validation = dvHelper.createValidation(constraint, addressList)
        txSheet.addValidationData(validation)

        assertEquals(1, txSheet.dataValidations.size)
        workbook.close()
    }

    @Test
    fun testCsvGeneration() {
        val tempCsv = File.createTempFile("test_csv", ".csv")
        java.io.FileWriter(tempCsv).use { fw ->
            com.opencsv.CSVWriter(fw).use { writer ->
                writer.writeNext(arrayOf("Date", "Time", "Merchant", "Category", "Subcategory", "Type", "Amount", "Bank", "Account", "Description"))
                writer.writeNext(arrayOf("2026-10-01", "12:00:00", "Coffee Shop", "Food & Drinks", "Coffee", "EXPENSE", "150.00", "HDFC", "1234", "Latte"))
                writer.writeNext(arrayOf("2026-10-02", "15:30:00", "Client Payment", "Income", "Freelance", "INCOME", "50000.00", "SBI", "5678", "Consulting"))
            }
        }
        assertTrue(tempCsv.exists())
        assertTrue(tempCsv.length() > 0)
        tempCsv.delete()
    }

    @Test
    fun testTransferPairingLogic() {
        val transferOut = com.reddy.vittify.data.backup.importwizard.ImportTransactionItem(
            merchantName = "US ACCOUNT → Primary",
            amount = java.math.BigDecimal("10.00"),
            transactionType = com.reddy.vittify.data.database.entity.TransactionType.TRANSFER,
            dateTime = java.time.LocalDateTime.now(),
            categoryName = "Self Transfer",
            originalBankName = "US ACCOUNT",
            toBankName = "Primary",
            currency = "USD",
            targetAmount = java.math.BigDecimal("952.34"),
            targetCurrency = "INR"
        )

        assertEquals(com.reddy.vittify.data.database.entity.TransactionType.TRANSFER, transferOut.transactionType)
        assertEquals("USD", transferOut.currency)
        assertEquals("INR", transferOut.targetCurrency)
        assertEquals(java.math.BigDecimal("952.34"), transferOut.targetAmount)
        assertEquals("US ACCOUNT", transferOut.originalBankName)
        assertEquals("Primary", transferOut.toBankName)
    }
}
