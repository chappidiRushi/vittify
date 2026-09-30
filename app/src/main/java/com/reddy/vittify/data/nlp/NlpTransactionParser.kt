package com.reddy.vittify.data.nlp

import android.util.Log
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.ai.AiPreferencesRepository
import com.reddy.vittify.data.ai.GeminiAiProvider
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

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
    val subcategory: String = ""
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

    suspend fun parse(input: String): ParsedTransactionDraft {
        if (input.isBlank()) return ParsedTransactionDraft()

        // 1. Try AI parsing if enabled & configured
        try {
            val isAiEnabled = aiPreferencesRepository.isAiEnabled.first()
            val geminiConfig = aiPreferencesRepository.geminiConfig.first()

            if (isAiEnabled && geminiConfig.isEnabled && geminiConfig.apiKey.isNotBlank()) {
                val aiResult = parseWithAi(input, geminiConfig.apiKey, geminiConfig.selectedModel)
                if (aiResult != null) {
                    Log.d(TAG, "Successfully parsed transaction using AI: $aiResult")
                    return aiResult
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AI transaction parsing failed, falling back to regex", e)
        }

        // 2. Fallback to Regex Parser
        return parseWithRegex(input)
    }

    private suspend fun parseWithAi(
        input: String,
        apiKey: String,
        model: String
    ): ParsedTransactionDraft? {
        val userAccounts = try {
            accountBalanceRepository.getAllLatestBalances().first()
        } catch (e: Exception) {
            emptyList()
        }

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

        val expenseCategories = userCategories.filter { !it.isIncome }
        val targetCategories = if (expenseCategories.isNotEmpty()) expenseCategories else userCategories

        val formattedCategories = targetCategories.joinToString("\n") { cat ->
            val subs = allSubcategories.filter { it.categoryId == cat.id }
            if (subs.isNotEmpty()) {
                "- Category: \"${cat.name}\" (Subcategories: ${subs.joinToString(", ") { "\"${it.name}\"" }})"
            } else {
                "- Category: \"${cat.name}\""
            }
        }

        val formattedAccounts = if (userAccounts.isNotEmpty()) {
            userAccounts.joinToString("\n") { acc ->
                "- Bank Name: \"${acc.bankName}\", Account Number/Last4: \"${acc.accountLast4}\", Alias Prompt/Custom ID: \"${acc.customId ?: "None"}\""
            }
        } else {
            "No saved bank accounts."
        }

        val prompt = """
You are an intelligent financial assistant extracting details from user's expense or income transaction text.

User Input: "$input"

Available Bank Accounts in user's app:
$formattedAccounts

Available Categories & Subcategories in user's app:
$formattedCategories

MANDATORY CATEGORY & SUBCATEGORY MATCHING INSTRUCTIONS:
1. You MUST match the user's item or purchase (e.g. "mangoes", "milk", "coffee", "tea", "uber", "movie ticket") to ONE of the Available Categories listed above.
2. The item name in user input DOES NOT need to match the category name directly. Match conceptually!
   - Example: "mangoes", "apples", or "banana" -> Category: "Groceries" (or "Food & Drinks"), Subcategory: "Fruits" (if "Fruits" exists under that category).
   - Example: "coffee" or "tea" -> Category: "Food & Drinks", Subcategory: "Tea & Coffee" (or "Eat out").
   - Example: "milk", "cheese", or "curd" -> Category: "Groceries", Subcategory: "Milk & Dairy" (or "Dairy").
3. You MUST pick the category name EXACTLY as spelled in the list above.
4. If a matching subcategory is listed under that category, you MUST select it and pick its name EXACTLY as spelled in the list above. If no specific subcategory fits, return empty string "".
5. If no reasonable category fits, default category to "Miscellaneous".

OTHER EXTRACTION INSTRUCTIONS:
1. Extract numeric amount (e.g., "500" from "paid 500 for mangoes from hdk"). Return digits and decimal points only.
2. Identify the merchant or item (e.g., "mangoes").
3. Determine transaction type (MUST be EXPENSE, INCOME, TRANSFER, CREDIT, or INVESTMENT. Default to EXPENSE).
4. Match bank account from Available Bank Accounts matching account name, last4, or alias/custom ID (e.g. "hdk" or "sb1" matches account with custom ID/alias "hdk" or "sb1"). Return exact Bank Name or Alias/Custom ID if matched, else empty string "".
5. Set notes to "$input".

Return ONLY a raw JSON object with no code blocks or markdown, using this exact format:
{
  "amount": "500",
  "merchant": "mangoes",
  "type": "EXPENSE",
  "bankName": "hdk",
  "category": "Groceries",
  "subcategory": "Fruits",
  "notes": "$input"
}
""".trimIndent()

        val response = geminiAiProvider.generateContent(apiKey, model, prompt)
        val result = response.getOrNull() ?: return null

        if (result.totalTokens > 0) {
            aiPreferencesRepository.recordRequestUsage(result.totalTokens)
        }

        val rawText = result.text.trim()
        if (rawText.isBlank()) return null

        // Clean json output (strip ```json or ``` blocks)
        val jsonText = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val json = JSONObject(jsonText)
            val amount = json.optString("amount", "")
            val merchant = json.optString("merchant", "")
            val typeStr = json.optString("type", "EXPENSE")
            val rawBankName = json.optString("bankName", "")
            val rawCategory = json.optString("category", "Miscellaneous")
            val rawSubcategory = json.optString("subcategory", "")
            val notes = json.optString("notes", input)

            val type = try {
                TransactionType.valueOf(typeStr.uppercase())
            } catch (_: Exception) {
                TransactionType.EXPENSE
            }

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

            // Resolve category against user categories using fuzzy matching
            val matchedCat = findBestMatchingCategory(userCategories, rawCategory)
            val resolvedCategory = matchedCat?.name ?: rawCategory

            // Resolve subcategory against user subcategories using fuzzy matching
            val matchedSub = if (matchedCat != null && rawSubcategory.isNotBlank()) {
                val subsForCat = allSubcategories.filter { it.categoryId == matchedCat.id }
                findBestMatchingSubcategory(subsForCat, rawSubcategory)
            } else null
            val resolvedSubcategory = matchedSub?.name ?: rawSubcategory

            ParsedTransactionDraft(
                amount = amount,
                merchant = merchant,
                type = type,
                bankName = resolvedBankName,
                notes = notes,
                category = resolvedCategory,
                subcategory = resolvedSubcategory
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

        // --- Amount ---
        val amount = extractAmount(lower)

        // --- Transaction Type ---
        val type = detectType(lower)

        // --- Merchant ---
        val merchant = extractMerchant(input, lower, type)

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
            subcategory = subcategory
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

        // 1c. Check subcategory keywords from SUBCATEGORY_KEYWORD_MAP
        for (sub in sortedSubcategories) {
            val keywords = SUBCATEGORY_KEYWORD_MAP[sub.name]
                ?: SUBCATEGORY_KEYWORD_MAP.entries.find { it.key.equals(sub.name, ignoreCase = true) }?.value
            if (keywords != null) {
                for (kw in keywords) {
                    val pattern = Regex("""\b${Regex.escape(kw.lowercase())}\b""", RegexOption.IGNORE_CASE)
                    if (pattern.containsMatchIn(lower)) {
                        val parentCat = userCategories.find { it.id == sub.categoryId }
                        if (parentCat != null) {
                            return Pair(parentCat.name, sub.name)
                        }
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
                val keywords = SUBCATEGORY_KEYWORD_MAP[sub.name]
                    ?: SUBCATEGORY_KEYWORD_MAP.entries.find { it.key.equals(sub.name, ignoreCase = true) }?.value
                if (keywords != null && keywords.any { Regex("""\b${Regex.escape(it.lowercase())}\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower) }) {
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
