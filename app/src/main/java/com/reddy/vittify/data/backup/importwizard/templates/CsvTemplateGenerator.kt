package com.reddy.vittify.data.backup.importwizard.templates

import android.content.Context
import android.net.Uri
import com.opencsv.CSVWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvTemplateGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generateTemplateFile(): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "vittify_sample_template.csv")
        FileWriter(file).use { fileWriter ->
            CSVWriter(fileWriter).use { csvWriter ->
                // Instructions / headers
                csvWriter.writeNext(arrayOf(
                    "Date",
                    "Time",
                    "Merchant",
                    "Category",
                    "Subcategory",
                    "Type",
                    "Amount",
                    "Bank",
                    "Account",
                    "Description"
                ))

                // Sample rows
                csvWriter.writeNext(arrayOf(
                    "2026-10-01",
                    "09:30:00",
                    "Starbucks Coffee",
                    "Food & Drinks",
                    "Coffee",
                    "EXPENSE",
                    "350.00",
                    "HDFC Bank",
                    "4321",
                    "Morning mocha"
                ))

                csvWriter.writeNext(arrayOf(
                    "2026-10-02",
                    "14:15:00",
                    "TechCorp Ltd",
                    "Income",
                    "Salary",
                    "INCOME",
                    "75000.00",
                    "State Bank of India",
                    "Salary",
                    "Monthly pay"
                ))

                csvWriter.writeNext(arrayOf(
                    "2026-10-03",
                    "19:45:00",
                    "Amazon.in",
                    "Shopping",
                    "Electronics",
                    "EXPENSE",
                    "1499.00",
                    "ICICI Bank",
                    "Coral CC",
                    "USB Cable & Adapter"
                ))

                csvWriter.writeNext(arrayOf(
                    "2026-10-04",
                    "11:00:00",
                    "Self Transfer",
                    "Other",
                    "",
                    "TRANSFER",
                    "5000.00",
                    "State Bank of India",
                    "Salary",
                    "Transfer to HDFC"
                ))
            }
        }
        file
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
}
