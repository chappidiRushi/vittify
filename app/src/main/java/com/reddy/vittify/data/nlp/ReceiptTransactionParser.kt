package com.reddy.vittify.data.nlp

import android.content.Context
import android.net.Uri
import android.util.Log
import com.reddy.vittify.data.ai.AiPreferencesRepository
import com.reddy.vittify.data.ai.GeminiAiProvider
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.utils.ReceiptImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for extracting transactional data from scanned receipt images using Google Gemini AI.
 * Extracts amounts, merchant, date/time, category, payment hints, and itemized lines matching
 * the user's category catalogue and account configuration.
 */
@Singleton
class ReceiptTransactionParser @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val aiPreferencesRepository: AiPreferencesRepository,
    private val geminiAiProvider: GeminiAiProvider,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "ReceiptTxnParser"

        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.ENGLISH)
        )

        private val DATE_ONLY_FORMATTERS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
        )
    }

    /**
     * Checks whether Gemini AI is enabled and configured with an API key.
     */
    suspend fun isAiConfigured(): Boolean {
        return try {
            val isAiEnabled = aiPreferencesRepository.isAiEnabled.first()
            val geminiConfig = aiPreferencesRepository.geminiConfig.first()
            isAiEnabled && geminiConfig.isEnabled && geminiConfig.apiKey.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Parses a receipt image from [imageUri] or [savedFile] using Gemini AI and returns a [ParsedTransactionDraft]
     * with grand total, merchant, date, categories, notes, and all parsed line items.
     */
    suspend fun parseReceipt(imageUri: Uri, savedFile: File? = null): Result<ParsedTransactionDraft> {
        val configured = isAiConfigured()
        if (!configured) {
            return Result.failure(
                IllegalStateException("Google Gemini AI is not configured. Please enter your API key in AI Settings.")
            )
        }

        // 1. Prepare image for AI: prefer local saved file to avoid content provider stream exhaustion
        val imageBytes = if (savedFile != null && savedFile.exists()) {
            ReceiptImageUtils.prepareReceiptImageForAi(context, savedFile)
        } else {
            ReceiptImageUtils.prepareReceiptImageForAi(context, imageUri)
        } ?: return Result.failure(Exception("Failed to read or process receipt image. Please try again."))

        val geminiConfig = aiPreferencesRepository.geminiConfig.first()

        // 2. Fetch context: accounts, categories, subcategories, rules
        val userAccounts = if (geminiConfig.includeBankAccounts) {
            try {
                accountBalanceRepository.getAllLatestBalances().first()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val userCategories = if (geminiConfig.includeCategories) {
            try {
                categoryRepository.getAllCategories().first()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val allSubcategories = if (geminiConfig.includeCategories) {
            try {
                subcategoryRepository.getAllSubcategories().first()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val categoriesSection = if (geminiConfig.includeCategories && userCategories.isNotEmpty()) {
            val expenseCategories = userCategories.filter { !it.isIncome }
            val targetCategories = if (expenseCategories.isNotEmpty()) expenseCategories else userCategories
            val formatted = targetCategories.joinToString("\n") { cat ->
                val subs = allSubcategories.filter { it.categoryId == cat.id }
                if (subs.isNotEmpty()) {
                    "- \"${cat.name}\" (Subcategories: ${subs.joinToString(", ") { "\"${it.name}\"" }})"
                } else {
                    "- \"${cat.name}\""
                }
            }
            "Available Categories & Subcategories in user's app:\n$formatted"
        } else ""

        val accountsSection = if (geminiConfig.includeBankAccounts && userAccounts.isNotEmpty()) {
            val formatted = userAccounts.joinToString("\n") { acc ->
                "- Bank: \"${acc.bankName}\", Last4: \"${acc.accountLast4}\", Alias: \"${acc.customId ?: ""}\""
            }
            "Available Bank Accounts in user's app:\n$formatted"
        } else ""

        val customRulesSection = if (geminiConfig.customRules.isNotBlank()) {
            "User Custom Rules:\n${geminiConfig.customRules.trim()}"
        } else ""

        val today = LocalDate.now()
        val todayDesc = "$today (${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }})"

        // 3. Build optimized receipt prompt
        val prompt = buildString {
            appendLine("You are an expert financial receipt parser. Extract all transactional data from this receipt image into strict JSON.")
            appendLine("Reference Date (Today): $todayDesc")
            appendLine()
            if (accountsSection.isNotBlank()) {
                appendLine(accountsSection)
                appendLine()
            }
            if (categoriesSection.isNotBlank()) {
                appendLine(categoriesSection)
                appendLine()
            }
            if (customRulesSection.isNotBlank()) {
                appendLine(customRulesSection)
                appendLine()
            }
            appendLine("Extraction Instructions:")
            appendLine("1. \"amount\": The grand total amount paid (numeric string only, digits and optional decimal point, e.g. \"42.50\"). Prioritize final \"Total\", \"Grand Total\", \"Amount Due\", or \"Amount Paid\" after discounts, taxes, and tips. Strip all currency symbols, spaces, and commas.")
            appendLine("2. \"merchant\": The store, business, merchant, or restaurant name (e.g. \"Costco\", \"Walmart\", \"McDonald's\"). Clean and concise; omit legal suffixes like LLC or branch phone numbers unless helpful.")
            appendLine("3. \"type\": \"EXPENSE\" (default) or \"INCOME\" (if return/refund receipt).")
            appendLine("4. \"date\": ISO format \"YYYY-MM-DDTHH:mm:ss\" or \"YYYY-MM-DD\". Parse the transaction date and time printed on the receipt. If only date is available, use \"YYYY-MM-DD\". If not present, return \"\".")
            appendLine("5. \"category\": Choose the single best conceptual match from the Available Categories list for the overall transaction (e.g. \"Groceries\", \"Food & Drinks\", \"Shopping\", \"Medical\").")
            appendLine("6. \"subcategory\": If the chosen category has subcategories, choose the closest matching subcategory, else \"\".")
            appendLine("7. \"bankName\": If a payment card, bank, or payment method is indicated (e.g. \"Visa ending in 1234\", \"Amex\", \"Cash\", \"HDFC\"), match with user's bank accounts or return bank name / card last 4, else \"\".")
            appendLine("8. \"notes\": Include receipt metadata: receipt/invoice number, tax amounts, payment mode, or brief summary.")
            appendLine("9. \"items\": If the receipt has multiple itemized products/lines, extract ALL items into this array. If no item breakdown exists, return an empty array [].")
            appendLine("   Each element in \"items\" MUST have:")
            appendLine("   - \"name\": Clean product/item description (include quantity if > 1, e.g. \"Whole Milk (x2)\").")
            appendLine("   - \"amount\": Final line total for this item (numeric string only, e.g. \"3.99\").")
            appendLine("   - \"category\": Conceptually match the item to one of the Available Categories (e.g. Groceries).")
            appendLine("   - \"subcategory\": Match to a subcategory under that category, or \"\".")
            appendLine()
            appendLine("Return ONLY a raw JSON object with keys: amount, merchant, type, category, subcategory, bankName, date, notes, items. Do not include markdown code blocks or explanations.")
        }

        val effectiveModel = geminiConfig.selectedModel.ifBlank { "gemini-1.5-flash" }

        // 4. Send multimodal request to Gemini
        return try {
            val response = geminiAiProvider.generateContentWithImage(
                apiKey = geminiConfig.apiKey,
                model = effectiveModel,
                prompt = prompt,
                imageBytes = imageBytes,
                mimeType = "image/jpeg"
            )

            val result = response.fold(
                onSuccess = { it },
                onFailure = { error ->
                    Log.e(TAG, "Gemini receipt parsing failed: ${error.message}", error)
                    return Result.failure(error)
                }
            )

            if (result.totalTokens > 0) {
                aiPreferencesRepository.recordRequestUsage(result.totalTokens)
            }

            val rawText = result.text.trim()
            if (rawText.isBlank()) {
                return Result.failure(Exception("AI returned empty content from receipt image."))
            }

            val jsonText = extractJsonObject(rawText)
                ?: return Result.failure(Exception("Could not extract structured JSON from AI receipt response."))

            val draft = parseJsonDraft(jsonText)
            Log.d(TAG, "Successfully parsed receipt: merchant=${draft.merchant}, amount=${draft.amount}, itemsCount=${draft.items.size}")
            Result.success(draft)
        } catch (e: Exception) {
            Log.e(TAG, "Error during receipt parsing", e)
            Result.failure(e)
        }
    }

    private fun extractJsonObject(rawText: String): String? {
        val trimmed = rawText.trim()
        val clean = if (trimmed.startsWith("```")) {
            trimmed.substringAfter("\n").substringBeforeLast("```").trim()
        } else trimmed

        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return clean.substring(start, end + 1).trim()
        }
        return null
    }

    private fun parseJsonDraft(jsonString: String): ParsedTransactionDraft {
        val json = JSONObject(jsonString)
        val rawAmount = json.optString("amount", "")
        var cleanAmount = rawAmount.replace(Regex("""[^0-9.]"""), "").trim()
        val merchant = json.optString("merchant", "").trim()
        val typeStr = json.optString("type", "EXPENSE")
        val rawBankName = json.optString("bankName", "").trim()
        val rawCategory = json.optString("category", "Miscellaneous").trim()
        val rawSubcategory = json.optString("subcategory", "").trim()
        val rawDate = json.optString("date", "").trim()
        val notes = json.optString("notes", "").trim()

        val type = try {
            TransactionType.valueOf(typeStr.uppercase())
        } catch (_: Exception) {
            TransactionType.EXPENSE
        }

        val parsedDate = parseReceiptDate(rawDate)

        // Parse itemized lines
        val itemsList = mutableListOf<ParsedReceiptItemDraft>()
        val jsonItems = json.optJSONArray("items")
        if (jsonItems != null) {
            for (i in 0 until jsonItems.length()) {
                val itemObj = jsonItems.optJSONObject(i) ?: continue
                val itemName = itemObj.optString("name", "").trim()
                val itemRawAmount = itemObj.optString("amount", "")
                val itemCleanAmount = itemRawAmount.replace(Regex("""[^0-9.]"""), "").trim()
                val itemCategory = itemObj.optString("category", rawCategory).trim().ifBlank { rawCategory }
                val itemSubcategory = itemObj.optString("subcategory", "").trim()

                if (itemName.isNotBlank() || itemCleanAmount.isNotBlank()) {
                    itemsList.add(
                        ParsedReceiptItemDraft(
                            name = itemName.ifBlank { "Item ${i + 1}" },
                            amount = itemCleanAmount.ifBlank { "0" },
                            category = itemCategory.ifBlank { "Miscellaneous" },
                            subcategory = itemSubcategory
                        )
                    )
                }
            }
        }

        // Fallback: If amount was empty or 0, calculate from items sum
        if ((cleanAmount.isBlank() || cleanAmount == "0" || cleanAmount == "0.0" || cleanAmount == "0.00") && itemsList.isNotEmpty()) {
            val sum = itemsList.fold(BigDecimal.ZERO) { acc, item ->
                acc + (item.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
            }
            if (sum > BigDecimal.ZERO) {
                cleanAmount = sum.stripTrailingZeros().toPlainString()
            }
        }

        return ParsedTransactionDraft(
            amount = cleanAmount,
            merchant = merchant,
            type = type,
            bankName = rawBankName,
            notes = notes,
            category = rawCategory.ifBlank { "Miscellaneous" },
            subcategory = rawSubcategory,
            date = parsedDate,
            items = itemsList
        )
    }

    private fun parseReceiptDate(rawDate: String): LocalDateTime? {
        if (rawDate.isBlank()) return null

        val trimmed = rawDate.trim()

        // 1. Try date-time formatters
        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter)
            } catch (_: Exception) { }
        }

        // 2. Try with space replaced with T
        if (trimmed.contains(' ') && !trimmed.contains('T')) {
            val isoCandidate = trimmed.replace(' ', 'T')
            try {
                return LocalDateTime.parse(isoCandidate)
            } catch (_: Exception) { }
        }

        // 3. Try date-only formatters and combine with current time
        val datePart = trimmed.substringBefore('T').substringBefore(' ')
        for (formatter in DATE_ONLY_FORMATTERS) {
            try {
                val date = LocalDate.parse(datePart, formatter)
                return date.atTime(LocalTime.now())
            } catch (_: Exception) { }
        }

        return null
    }
}
