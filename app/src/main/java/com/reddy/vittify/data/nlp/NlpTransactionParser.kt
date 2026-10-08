package com.reddy.vittify.data.nlp

import android.util.Log
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.ai.AiPreferencesRepository
import com.reddy.vittify.data.ai.GeminiAiProvider
import kotlinx.coroutines.flow.first
import com.reddy.vittify.domain.catalogue.CategoryItemCatalogue
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

enum class NlpParsingMode {
    AI,
    REGEX
}

/**
 * Parsed draft that pre-fills the AddTransaction screen.
 * All fields are optional — empty string means "not detected".
 */
data class ParsedTransactionDraft(
    val amount: String = "",
    val merchant: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val bankName: String = "",          // raw bank name hint from text
    val notes: String = "",
    val category: String = "Miscellaneous",
    val subcategory: String = "",
    val date: LocalDateTime? = null
)



/**
 * Uses the Gemini LLM (if configured) to extract transaction fields from
 * a natural language string. Passes available bank accounts and expense categories
 * to let AI match accounts & categories conceptually.
 * Falls back to regex parser if AI is not configured or fails.
 */
@Singleton
class NlpTransactionParser @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val aiPreferencesRepository: AiPreferencesRepository,
    private val geminiAiProvider: GeminiAiProvider
) {

    companion object {
        private const val TAG = "NlpTransactionParser"

        // Regex patterns for fallback parser
        private val AMOUNT_REGEX = Regex("""(?:rs\.?|inr|₹|\$|€|£|usd|eur|gbp)?\s*(\d+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE)
        private val AMOUNT_SUFFIX_REGEX = Regex("""(\d+(?:[.,]\d+)?)\s*(?:rs\.?|inr|₹|\$|€|£|usd|eur|gbp)""", RegexOption.IGNORE_CASE)

        private val INCOME_KEYWORDS = setOf("received", "credited", "income", "salary", "earned", "got", "deposit", "refund", "cashback")
        private val EXPENSE_KEYWORDS = setOf("paid", "spent", "debited", "bought", "purchased", "charged", "deducted", "payment")
        private val INVESTMENT_KEYWORDS = setOf("invested", "mutual fund", "sip", "stocks", "shares", "mf", "gold")
        private val TRANSFER_KEYWORDS = setOf("transferred", "sent to", "transfer", "moved")
        private val CREDIT_KEYWORDS = setOf("credit card", "emi", "card payment", "creditcard")

        // Category keyword hints mapped to actual system/DB categories
        private val CATEGORY_HINTS = mapOf(
            "Food & Drinks" to listOf("food", "dining", "restaurant", "lunch", "dinner", "breakfast", "eat", "meal", "zomato", "swiggy", "uber eats", "coffee", "cafe", "tea", "starbucks"),
            "Shopping" to listOf("shopping", "amazon", "flipkart", "clothes", "apparel", "shoes", "mall", "shop", "myntra", "meesho"),
            "Transport" to listOf("uber", "ola", "rapido", "cab", "taxi", "petrol", "fuel", "bus", "metro", "train", "auto", "fastag", "toll"),
            "Entertainment" to listOf("movie", "cinema", "game", "theater", "show", "concert", "bowling"),
            "Medical" to listOf("medical", "medicine", "pharmacy", "hospital", "doctor", "health", "clinic", "dentist", "pill"),
            "Fitness" to listOf("gym", "fitness", "yoga", "workout", "sports", "running"),
            "Bill" to listOf("electricity", "water", "gas", "internet", "broadband", "mobile", "recharge", "bill", "rent"),
            "Subscription" to listOf("netflix", "spotify", "youtube premium", "prime video", "subscription", "hotstar", "disney+"),
            "Investment" to listOf("mutual fund", "sip", "stocks", "shares", "investment", "mf", "gold", "nps"),
            "Income" to listOf("salary", "income", "freelance", "rent received", "dividend", "cashback"),
            "Self Transfer" to listOf("transfer", "transferred", "sent to myself", "moved"),
            "Groceries" to listOf("groceries", "grocery", "milk", "vegetables", "fruits", "zepto", "blinkit", "bigbasket", "instamart"),
            "Insurance" to listOf("insurance", "lic", "premium"),
            "Tax" to listOf("tax", "gst", "income tax"),
            "EMI" to listOf("emi", "loan", "home loan", "car loan"),
            "Travel" to listOf("travel", "hotel", "airbnb", "flight", "vacation", "trip"),
            "Support" to listOf("mom", "dad", "family", "sister", "brother", "wife", "husband", "son", "daughter", "support"),
        )

        private val CATEGORY_ALIASES = mapOf(
            "food" to "Food & Drinks",
            "drinks" to "Food & Drinks",
            "dining" to "Food & Drinks",
            "transportation" to "Transport",
            "transport" to "Transport",
            "health" to "Medical",
            "healthcare" to "Medical",
            "utilities" to "Bill",
            "utility" to "Bill",
            "bills" to "Bill",
            "subscription" to "Subscription",
            "subscriptions" to "Subscription",
            "investments" to "Investment",
            "self-transfer" to "Self Transfer"
        )

        private val SUBCATEGORY_KEYWORD_MAP = mapOf(
            "Dairy" to listOf("dairy", "milk", "curd", "cheese", "paneer", "butter", "yogurt", "ghee"),
            "Fruits" to listOf("fruit", "fruits", "apple", "apples", "banana", "bananas", "mango", "mangoes", "orange", "oranges"),
            "Vegetables" to listOf("vegetable", "vegetables", "veggie", "veggies", "tomato", "tomatoes", "potato", "potatoes", "onion", "onions"),
            "Staples" to listOf("staples", "rice", "dal", "atta", "flour", "wheat", "oil", "sugar", "salt"),
            "Meat" to listOf("meat", "chicken", "mutton", "fish", "prawns", "seafood", "pork", "beef"),
            "Eggs" to listOf("egg", "eggs"),
            "Bakery" to listOf("bakery", "bread", "cake", "pastry", "biscuit", "cookies", "toast"),
            "Zepto" to listOf("zepto", "blinkit", "instamart", "bigbasket"),
            "Fuel" to listOf("fuel", "petrol", "diesel", "cng"),
            "Dining" to listOf("dining", "dinner", "lunch", "breakfast", "restaurant", "cafe", "eat out"),
            "Coffee" to listOf("coffee", "tea", "starbucks", "chai"),
            "Medicine" to listOf("medicine", "medicines", "medical", "pharmacy", "doctor", "tablet", "pills", "hospital"),
            "Rent" to listOf("rent", "house rent"),
            "Electricity" to listOf("electricity", "power", "electric bill", "eb bill"),
            "Internet" to listOf("internet", "wifi", "broadband"),
            "Mobile" to listOf("recharge", "mobile recharge", "phone bill"),
            "Movie" to listOf("movie", "movies", "cinema", "theatre", "theater"),
            "Gym" to listOf("gym", "fitness", "workout"),
            "Flight" to listOf("flight", "flights", "airline", "airways"),
            "Hotel" to listOf("hotel", "resort", "airbnb", "stay")
        )
    }

    suspend fun parse(
        input: String,
        onStatusChange: ((NlpParsingMode) -> Unit)? = null,
        onAiError: ((String) -> Unit)? = null
    ): ParsedTransactionDraft? {
        if (input.isBlank()) return ParsedTransactionDraft()

        // 1. Try AI parsing if enabled & configured
        val aiConfigured = isAiConfigured()
        if (aiConfigured) {
            onStatusChange?.invoke(NlpParsingMode.AI)
            try {
                val geminiConfig = aiPreferencesRepository.geminiConfig.first()
                val aiResult = parseWithAi(input, geminiConfig)
                if (aiResult != null) {
                    Log.d(TAG, "Successfully parsed transaction using AI: $aiResult")
                    return aiResult
                }
                Log.w(TAG, "AI returned null draft, falling back to regex")
            } catch (e: Exception) {
                Log.e(TAG, "AI transaction parsing failed: ${e.message}", e)
                if (onAiError != null) {
                    onAiError(e.message ?: "AI transaction parsing failed")
                    return null
                }
                Log.w(TAG, "No onAiError callback provided, falling back to regex")
            }
        }

        // 2. Fallback to Regex Parser
        onStatusChange?.invoke(NlpParsingMode.REGEX)
        return parseWithRegex(input)
    }

    private suspend fun isAiConfigured(): Boolean {
        return try {
            val isAiEnabled = aiPreferencesRepository.isAiEnabled.first()
            val geminiConfig = aiPreferencesRepository.geminiConfig.first()
            isAiEnabled && geminiConfig.isEnabled && geminiConfig.apiKey.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    private fun extractJsonObject(rawText: String): String? {
        val start = rawText.indexOf('{')
        val end = rawText.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return rawText.substring(start, end + 1).trim()
        }
        return null
    }

    private suspend fun parseWithAi(
        input: String,
        config: com.reddy.vittify.data.ai.GeminiConfig
    ): ParsedTransactionDraft? {
        val userAccounts = if (config.includeBankAccounts) {
            try {
                accountBalanceRepository.getAllLatestBalances().first()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val userCategories = if (config.includeCategories) {
            try {
                categoryRepository.getAllCategories().first()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val allSubcategories = if (config.includeCategories) {
            try {
                subcategoryRepository.getAllSubcategories().first()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val categoriesSection = if (config.includeCategories && userCategories.isNotEmpty()) {
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

        val accountsSection = if (config.includeBankAccounts && userAccounts.isNotEmpty()) {
            val formatted = userAccounts.joinToString("\n") { acc ->
                "- Bank: \"${acc.bankName}\", Last4: \"${acc.accountLast4}\", Alias: \"${acc.customId ?: ""}\""
            }
            "Available Bank Accounts in user's app:\n$formatted"
        } else ""

        val customRulesSection = if (config.customRules.isNotBlank()) {
            "User Custom Rules:\n${config.customRules.trim()}"
        } else ""

        val today = LocalDate.now()
        val todayDesc = "$today (${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }})"

        val prompt = buildString {
            appendLine("Extract financial transaction details from the user's text into strict JSON.")
            appendLine("User Text: \"$input\"")
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
            appendLine("Rules:")
            appendLine("1. amount: numeric value only (digits and decimal), e.g. \"500\", \"45.50\".")
            appendLine("2. merchant: person, merchant, or item name.")
            appendLine("3. type: EXPENSE, INCOME, TRANSFER, CREDIT, or INVESTMENT (default EXPENSE).")
            if (config.includeBankAccounts) {
                appendLine("4. bankName: exact Bank Name or Alias matching user's bank accounts, else \"\".")
            } else {
                appendLine("4. bankName: bank name mentioned in text or \"\".")
            }
            if (config.includeCategories) {
                appendLine("5. category: conceptually match the item to one of the Available Categories exactly, default \"Miscellaneous\".")
                appendLine("6. subcategory: match to a subcategory listed under the chosen category, else \"\".")
            } else {
                appendLine("5. category: common category (e.g. Food & Drinks, Groceries, Shopping, Transport, Bill, Entertainment, Medical, Miscellaneous).")
                appendLine("6. subcategory: relevant subcategory or \"\".")
            }
            appendLine("7. date: ISO format YYYY-MM-DD (e.g. \"2024-03-15\") if a specific date or relative date (such as \"yesterday\", \"last Monday\", \"Tuesday\", \"3 days ago\", etc.) is mentioned. Calculate relative dates using the Reference Date. If no date is mentioned or implied, return \"\".")
            appendLine("8. notes: \"$input\"")
            appendLine()
            appendLine("Return ONLY a raw JSON object with keys: amount, merchant, type, bankName, category, subcategory, date, notes. No explanation, no markdown.")
        }

        val effectiveModel = config.selectedModel.ifBlank { "gemini-1.5-flash" }
        val response = geminiAiProvider.generateContent(config.apiKey, effectiveModel, prompt)
        val result = response.fold(
            onSuccess = { it },
            onFailure = { error ->
                Log.e(TAG, "Gemini generateContent call failed: ${error.message}", error)
                throw error
            }
        )

        if (result.totalTokens > 0) {
            aiPreferencesRepository.recordRequestUsage(result.totalTokens)
        }

        val rawText = result.text.trim()
        if (rawText.isBlank()) {
            Log.w(TAG, "AI returned empty text content")
            return null
        }

        val jsonText = extractJsonObject(rawText)
        if (jsonText.isNullOrBlank()) {
            Log.w(TAG, "Could not extract JSON object from AI response: $rawText")
            return null
        }

        return try {
            val json = JSONObject(jsonText)
            val rawAmount = json.optString("amount", "")
            val cleanAmount = rawAmount.replace(Regex("""[^0-9.]"""), "").trim()
            val merchant = json.optString("merchant", "")
            val typeStr = json.optString("type", "EXPENSE")
            val rawBankName = json.optString("bankName", "")
            val rawCategory = json.optString("category", "Miscellaneous")
            val rawSubcategory = json.optString("subcategory", "")
            val rawDate = json.optString("date", "")
            val notes = json.optString("notes", input)

            val type = try {
                TransactionType.valueOf(typeStr.uppercase())
            } catch (_: Exception) {
                TransactionType.EXPENSE
            }

            val parsedDate = if (rawDate.isNotBlank()) {
                NlpDateParser.parseAiDate(rawDate)
            } else {
                NlpDateParser.extractDateAndCleanInput(input).date
            }
            val transactionDateTime = parsedDate?.atTime(LocalTime.now())

            // Resolve bankName against actual user accounts if possible
            val resolvedBankName = if (rawBankName.isNotBlank()) {
                val matchedAcc = userAccounts.firstOrNull { acc ->
                    acc.customId?.equals(rawBankName, ignoreCase = true) == true ||
                    acc.bankName.equals(rawBankName, ignoreCase = true) ||
                    acc.accountLast4.equals(rawBankName, ignoreCase = true) ||
                    acc.bankName.contains(rawBankName, ignoreCase = true) ||
                    rawBankName.contains(acc.bankName, ignoreCase = true)
                }
                matchedAcc?.bankName ?: rawBankName
            } else {
                ""
            }

            val effectiveBankName = if (resolvedBankName.isNotBlank()) {
                resolvedBankName
            } else if (!config.includeBankAccounts) {
                val localAccounts = try { accountBalanceRepository.getAllLatestBalances().first() } catch (e: Exception) { emptyList() }
                extractBankName(input.lowercase(), localAccounts)
            } else {
                ""
            }

            // Resolve category against user categories
            val (effectiveCategory, effectiveSubcategory) = if (config.includeCategories) {
                val matchedCat = findBestMatchingCategory(userCategories, rawCategory)
                val resolvedCat = matchedCat?.name ?: rawCategory
                val matchedSub = if (matchedCat != null && rawSubcategory.isNotBlank()) {
                    val subsForCat = allSubcategories.filter { it.categoryId == matchedCat.id }
                    findBestMatchingSubcategory(subsForCat, rawSubcategory)
                } else null
                Pair(resolvedCat, matchedSub?.name ?: rawSubcategory)
            } else {
                val localCats = try { categoryRepository.getAllCategories().first() } catch (e: Exception) { emptyList() }
                val localSubs = try { subcategoryRepository.getAllSubcategories().first() } catch (e: Exception) { emptyList() }
                val matched = findBestMatchingCategory(localCats, rawCategory)
                if (matched != null) {
                    val subsForCat = localSubs.filter { it.categoryId == matched.id }
                    val matchedSub = findBestMatchingSubcategory(subsForCat, rawSubcategory)
                    Pair(matched.name, matchedSub?.name ?: rawSubcategory)
                } else {
                    detectCategoryAndSubcategory(input.lowercase(), type, localCats, localSubs)
                }
            }

            ParsedTransactionDraft(
                amount = cleanAmount,
                merchant = merchant,
                type = type,
                bankName = effectiveBankName,
                notes = notes,
                category = effectiveCategory,
                subcategory = effectiveSubcategory,
                date = transactionDateTime
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JSON response from AI: $jsonText", e)
            null
        }
    }

    private fun findBestMatchingCategory(
        categories: List<com.reddy.vittify.data.database.entity.CategoryEntity>,
        rawName: String
    ): com.reddy.vittify.data.database.entity.CategoryEntity? {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return null

        // 1. Exact case-insensitive match
        categories.find { it.name.equals(trimmed, ignoreCase = true) }?.let { return it }

        // 2. Normalized match
        val normRaw = normalizeForMatching(trimmed)
        categories.find { normalizeForMatching(it.name) == normRaw }?.let { return it }

        // 3. Contains match
        categories.find {
            val normCat = normalizeForMatching(it.name)
            normCat.contains(normRaw) || normRaw.contains(normCat)
        }?.let { return it }

        return null
    }

    private fun findBestMatchingSubcategory(
        subcategories: List<com.reddy.vittify.data.database.entity.SubcategoryEntity>,
        rawName: String
    ): com.reddy.vittify.data.database.entity.SubcategoryEntity? {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return null

        // 1. Exact case-insensitive match
        subcategories.find { it.name.equals(trimmed, ignoreCase = true) }?.let { return it }

        // 2. Normalized match
        val normRaw = normalizeForMatching(trimmed)
        subcategories.find { normalizeForMatching(it.name) == normRaw }?.let { return it }

        // 3. Contains match
        subcategories.find {
            val normSub = normalizeForMatching(it.name)
            normSub.contains(normRaw) || normRaw.contains(normSub)
        }?.let { return it }

        return null
    }

    private fun normalizeForMatching(text: String): String {
        return text.lowercase()
            .replace("&", "and")
            .replace(Regex("[^a-z0-9]"), "")
    }



    suspend fun parseWithRegex(input: String): ParsedTransactionDraft {
        val lower = input.lowercase()
        val userCategories = try {
            categoryRepository.getAllCategories().first()
        } catch (e: Exception) {
            emptyList()
        }
        val allSubcategories = try {
            subcategoryRepository.getAllSubcategories().first()
        } catch (e: Exception) {
            emptyList()
        }
        val userAccounts = try {
            accountBalanceRepository.getAllLatestBalances().first()
        } catch (e: Exception) {
            emptyList()
        }

        // --- Date & Sanitized Input for Merchant ---
        val dateResult = NlpDateParser.extractDateAndCleanInput(input)
        val transactionDateTime = dateResult.date?.atTime(LocalTime.now())
        val textWithoutDate = dateResult.cleanedText
        val lowerWithoutDate = textWithoutDate.lowercase()

        // --- Amount ---
        val amount = extractAmount(lower)

        // --- Transaction Type ---
        val type = detectType(lower)

        // --- Merchant (using text cleaned of date words) ---
        val merchant = extractMerchant(textWithoutDate, lowerWithoutDate, type)

        // --- Bank name ---
        val bankName = extractBankName(lower, userAccounts)

        // --- Category & Subcategory (Subcategory has high priority!) ---
        val (category, subcategory) = detectCategoryAndSubcategory(lower, type, userCategories, allSubcategories)

        // --- Notes ---
        val notes = input.trim()

        return ParsedTransactionDraft(
            amount = amount,
            merchant = merchant,
            type = type,
            bankName = bankName,
            notes = notes,
            category = category,
            subcategory = subcategory,
            date = transactionDateTime
        )
    }

    private fun extractAmount(lower: String): String {
        // Try prefix: "₹500", "$500", "rs 500", "inr 500"
        AMOUNT_REGEX.find(lower)?.groups?.get(1)?.value?.let { raw ->
            val clean = raw.replace(",", "")
            if (clean.toDoubleOrNull() != null) return clean
        }
        // Try suffix: "500 rs", "500 inr"
        AMOUNT_SUFFIX_REGEX.find(lower)?.groups?.get(1)?.value?.let { raw ->
            val clean = raw.replace(",", "")
            if (clean.toDoubleOrNull() != null) return clean
        }
        // Plain number fallback
        Regex("""\b(\d+(?:\.\d+)?)\b""").find(lower)?.groups?.get(1)?.value?.let { raw ->
            if (raw.toDoubleOrNull() != null) return raw
        }
        return ""
    }

    private fun detectType(lower: String): TransactionType {
        if (TRANSFER_KEYWORDS.any { lower.contains(it) }) return TransactionType.TRANSFER
        if (CREDIT_KEYWORDS.any { lower.contains(it) }) return TransactionType.CREDIT
        if (INVESTMENT_KEYWORDS.any { lower.contains(it) }) return TransactionType.INVESTMENT
        if (INCOME_KEYWORDS.any { lower.contains(it) }) return TransactionType.INCOME
        return TransactionType.EXPENSE
    }

    private fun extractMerchant(original: String, lower: String, type: TransactionType): String {
        // Look for "at <Merchant>" or "for <Merchant>" or "to <Merchant>" or "from <merchant>"
        val patterns = listOf(
            Regex("""(?:at|@)\s+([A-Za-z0-9 &'-]+?)(?:\s+(?:from|using|via|with|on|bank|account|by)\b|$)""", RegexOption.IGNORE_CASE),
            Regex("""for\s+([A-Za-z0-9 &'-]+?)(?:\s+(?:from|using|via|with|on|bank|account|by)\b|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:to|sent to)\s+([A-Za-z0-9 &'-]+?)(?:\s+(?:from|using|via|with|on|bank|account|by)\b|$)""", RegexOption.IGNORE_CASE),
        )
        for (p in patterns) {
            p.find(original)?.groups?.get(1)?.value?.trim()?.let {
                if (it.length >= 2) return it
            }
        }

        // Fallback: If no preposition pattern matched, extract remaining word tokens
        val cleaned = lower
            .replace(AMOUNT_REGEX, " ")
            .replace(AMOUNT_SUFFIX_REGEX, " ")
            .replace(Regex("""\b(rs\.?|inr|usd|eur|gbp|paid|spent|debited|credited|bought|purchased|using|via|from|to|at|for|bank|account|card|wallet|income|received|transferred)\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""[^a-zA-Z0-9\s]"""), " ")
            .trim()
            .split(Regex("""\s+"""))
            .filter { it.length >= 2 }

        if (cleaned.isNotEmpty() && cleaned.size <= 3) {
            val candidate = cleaned.joinToString(" ")
            val idx = lower.indexOf(candidate)
            if (idx >= 0 && idx + candidate.length <= original.length) {
                return original.substring(idx, idx + candidate.length).trim()
            }
            return candidate.replaceFirstChar { it.uppercase() }
        }

        return ""
    }

    private fun extractBankName(lower: String, userAccounts: List<com.reddy.vittify.data.database.entity.AccountBalanceEntity> = emptyList()): String {
        // Try to match custom account IDs first
        userAccounts.forEach { account ->
            account.customId?.let { id ->
                if (lower.contains(id.lowercase())) return account.bankName
            }
        }

        // Look for "from <bank> bank" or "using <bank>" patterns
        val bankPattern = Regex(
            """(?:from|using|via|through|with|in|at)\s+([A-Za-z]+(?:\s+[A-Za-z]+)?)\s*(?:bank|account|card|wallet)?""",
            RegexOption.IGNORE_CASE
        )
        bankPattern.find(lower)?.groups?.get(1)?.value?.trim()?.let { candidate ->
            // Filter out common non-bank words
            val stopWords = setOf("my", "the", "a", "an", "this", "that", "which", "sbi", "hdfc", "icici", "axis", "kotak") // added common banks as well just in case they are not in stop words
            if (!stopWords.contains(candidate.lowercase()) && candidate.length >= 2) {
                return candidate
            }
        }
        return ""
    }

    private fun detectCategoryAndSubcategory(
        lower: String,
        type: TransactionType,
        userCategories: List<com.reddy.vittify.data.database.entity.CategoryEntity>,
        allSubcategories: List<com.reddy.vittify.data.database.entity.SubcategoryEntity>
    ): Pair<String, String> {
        // 1. Check Subcategories first (Subcategories have high priority!)
        val sortedSubcategories = allSubcategories.sortedByDescending { it.name.length }

        // 1a. Direct word boundary match on subcategory name (case-insensitive)
        for (sub in sortedSubcategories) {
            val subNameLower = sub.name.lowercase().trim()
            if (subNameLower.length >= 2) {
                val pattern = Regex("""\b${Regex.escape(subNameLower)}\b""", RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(lower)) {
                    val parentCat = userCategories.find { it.id == sub.categoryId }
                    if (parentCat != null) {
                        return Pair(parentCat.name, sub.name)
                    }
                }
            }
        }

        // 1b. Check compound subcategories by parts (e.g., "Milk & Dairy", "Eat Out")
        for (sub in sortedSubcategories) {
            val parts = sub.name.split(Regex("""[&/,\-]|\band\b""", RegexOption.IGNORE_CASE))
                .map { it.trim().lowercase() }
                .filter { it.length >= 3 }
            for (part in parts) {
                val pattern = Regex("""\b${Regex.escape(part)}\b""", RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(lower)) {
                    val parentCat = userCategories.find { it.id == sub.categoryId }
                    if (parentCat != null) {
                        return Pair(parentCat.name, sub.name)
                    }
                }
            }
        }

        // 1c. Check subcategory keywords from SUBCATEGORY_KEYWORD_MAP & CategoryItemCatalogue
        for (sub in sortedSubcategories) {
            val parentCat = userCategories.find { it.id == sub.categoryId }
            val catalogueKeywords = CategoryItemCatalogue.getItemsForSubcategory(parentCat?.name, sub.name)
            val legacyKeywords = SUBCATEGORY_KEYWORD_MAP[sub.name]
                ?: SUBCATEGORY_KEYWORD_MAP.entries.find { it.key.equals(sub.name, ignoreCase = true) }?.value
                ?: emptyList()
            val allKeywords = (catalogueKeywords + legacyKeywords).distinct()

            for (kw in allKeywords) {
                val pattern = Regex("""\b${Regex.escape(kw.lowercase())}\b""", RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(lower)) {
                    if (parentCat != null) {
                        return Pair(parentCat.name, sub.name)
                    }
                }
            }
        }

        // 2. Check Category match
        val matchedCategoryName = detectCategory(lower, type, userCategories)
        val matchedCatEntity = userCategories.find { it.name.equals(matchedCategoryName, ignoreCase = true) }

        // If category matched, check if any of its subcategories match keywords or tokens in input
        if (matchedCatEntity != null) {
            val catSubs = allSubcategories.filter { it.categoryId == matchedCatEntity.id }
            for (sub in catSubs) {
                val subNameLower = sub.name.lowercase().trim()
                if (subNameLower.length >= 2) {
                    val pattern = Regex("""\b${Regex.escape(subNameLower)}\b""", RegexOption.IGNORE_CASE)
                    if (pattern.containsMatchIn(lower)) {
                        return Pair(matchedCatEntity.name, sub.name)
                    }
                }
                val catalogueKeywords = CategoryItemCatalogue.getItemsForSubcategory(matchedCatEntity.name, sub.name)
                val legacyKeywords = SUBCATEGORY_KEYWORD_MAP[sub.name]
                    ?: SUBCATEGORY_KEYWORD_MAP.entries.find { it.key.equals(sub.name, ignoreCase = true) }?.value
                    ?: emptyList()
                val allKeywords = (catalogueKeywords + legacyKeywords).distinct()
                if (allKeywords.any { Regex("""\b${Regex.escape(it.lowercase())}\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower) }) {
                    return Pair(matchedCatEntity.name, sub.name)
                }
            }
            return Pair(matchedCatEntity.name, "")
        }

        return Pair(matchedCategoryName, "")
    }

    private fun detectCategory(lower: String, type: TransactionType, userCategories: List<com.reddy.vittify.data.database.entity.CategoryEntity> = emptyList()): String {
        val incomeCat = userCategories.find { it.name.equals("Income", ignoreCase = true) }?.name
        val investmentCat = userCategories.find { it.name.equals("Investment", ignoreCase = true) }?.name
        val transferCat = userCategories.find { it.name.equals("Self Transfer", ignoreCase = true) }?.name
        val shoppingCat = userCategories.find { it.name.equals("Shopping", ignoreCase = true) }?.name
        val miscCat = userCategories.find { it.name.equals("Miscellaneous", ignoreCase = true) }?.name ?: "Miscellaneous"

        if (type == TransactionType.INCOME && incomeCat != null) return incomeCat
        if (type == TransactionType.INVESTMENT && investmentCat != null) return investmentCat
        if (type == TransactionType.TRANSFER && transferCat != null) return transferCat
        if (type == TransactionType.CREDIT && shoppingCat != null) return shoppingCat

        // Try to match user categories first
        userCategories.forEach { cat ->
            if (lower.contains(cat.name.lowercase())) return cat.name
        }

        for ((category, keywords) in CATEGORY_HINTS) {
            if (keywords.any { lower.contains(it) }) {
                val matched = userCategories.find { it.name.equals(category, ignoreCase = true) }
                if (matched != null) return matched.name
            }
        }
        return miscCat
    }
}
