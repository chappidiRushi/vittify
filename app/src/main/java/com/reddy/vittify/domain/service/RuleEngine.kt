package com.reddy.vittify.domain.service

import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.domain.model.rule.*
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleEngine @Inject constructor(
    private val subcategoryRepository: SubcategoryRepository,
    private val categoryRepository: CategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun evaluateRules(
        transaction: TransactionEntity,
        smsText: String?,
        rules: List<TransactionRule>
    ): Pair<TransactionEntity, List<RuleApplication>> {
        val sortedRules = rules
            .filter { it.isActive }
            .sortedBy { it.priority }

        var modifiedTransaction = transaction
        val applications = mutableListOf<RuleApplication>()

        for (rule in sortedRules) {
            if (evaluateConditions(modifiedTransaction, smsText, rule.conditions)) {
                val (newTransaction, fieldMods) = applyActions(modifiedTransaction, rule.actions)

                if (fieldMods.isNotEmpty()) {
                    modifiedTransaction = newTransaction
                    applications.add(
                        RuleApplication(
                            ruleId = rule.id,
                            ruleName = rule.name,
                            transactionId = transaction.id.toString(),
                            fieldsModified = fieldMods,
                            appliedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        return modifiedTransaction to applications
    }

    /**
     * Evaluates rules for a specific transaction type.
     * This is a convenience method that pre-filters rules based on transaction type.
     */
    suspend fun evaluateRulesForType(
        transaction: TransactionEntity,
        smsText: String?,
        rules: List<TransactionRule>,
        type: TransactionType
    ): Pair<TransactionEntity, List<RuleApplication>> {
        // Pre-filter rules that apply to this transaction type
        val applicableRules = rules.filter { rule ->
            val hasTypeCondition = rule.conditions.any { it.field == TransactionField.TYPE }
            if (!hasTypeCondition) {
                true // Rule applies to all transaction types
            } else {
                // Check if any TYPE condition matches this transaction's type
                rule.conditions.any { condition ->
                    condition.field == TransactionField.TYPE &&
                    when (condition.operator) {
                        ConditionOperator.EQUALS -> condition.value.equals(type.name, ignoreCase = true)
                        ConditionOperator.IN -> condition.value.split(",")
                            .map { it.trim() }
                            .any { it.equals(type.name, ignoreCase = true) }
                        ConditionOperator.NOT_EQUALS -> !condition.value.equals(type.name, ignoreCase = true)
                        ConditionOperator.NOT_IN -> !condition.value.split(",")
                            .map { it.trim() }
                            .any { it.equals(type.name, ignoreCase = true) }
                        else -> false
                    }
                }
            }
        }

        return evaluateRules(transaction, smsText, applicableRules)
    }

    /**
     * Checks if a transaction should be blocked based on blocking rules.
     * Returns the first blocking rule that matches, or null if no blocking rules match.
     */
    suspend fun shouldBlockTransaction(
        transaction: TransactionEntity,
        smsText: String?,
        rules: List<TransactionRule>
    ): TransactionRule? {
        // Filter for rules that have a BLOCK action
        val blockingRules = rules
            .filter { it.isActive }
            .filter { rule ->
                rule.actions.any { action -> action.actionType == ActionType.BLOCK }
            }
            .sortedBy { it.priority }

        // Return the first rule that matches the conditions
        for (rule in blockingRules) {
            if (evaluateConditions(transaction, smsText, rule.conditions)) {
                return rule
            }
        }

        return null
    }

    private suspend fun evaluateConditions(
        transaction: TransactionEntity,
        smsText: String?,
        conditions: List<RuleCondition>
    ): Boolean {
        // An empty conditions list means "match all transactions" (catch-all rule)
        if (conditions.isEmpty()) return true

        // KISS: Simply AND all conditions together
        // Each condition must be true for the rule to apply
        for (condition in conditions) {
            val conditionResult = evaluateCondition(transaction, smsText, condition)
            if (!conditionResult) {
                return false // If any condition is false, rule doesn't match
            }
        }

        return true // All conditions matched
    }

    private suspend fun evaluateCondition(
        transaction: TransactionEntity,
        smsText: String?,
        condition: RuleCondition
    ): Boolean {
        val fieldValue = getFieldValue(transaction, smsText, condition.field)

        return when (condition.operator) {
            ConditionOperator.EQUALS -> fieldValue.equals(condition.value, ignoreCase = true)
            ConditionOperator.NOT_EQUALS -> !fieldValue.equals(condition.value, ignoreCase = true)
            ConditionOperator.CONTAINS -> fieldValue.contains(condition.value, ignoreCase = true)
            ConditionOperator.NOT_CONTAINS -> !fieldValue.contains(condition.value, ignoreCase = true)
            ConditionOperator.STARTS_WITH -> fieldValue.startsWith(condition.value, ignoreCase = true)
            ConditionOperator.ENDS_WITH -> fieldValue.endsWith(condition.value, ignoreCase = true)
            ConditionOperator.LESS_THAN -> compareNumeric(fieldValue, condition.value) < 0
            ConditionOperator.GREATER_THAN -> compareNumeric(fieldValue, condition.value) > 0
            ConditionOperator.LESS_THAN_OR_EQUAL -> compareNumeric(fieldValue, condition.value) <= 0
            ConditionOperator.GREATER_THAN_OR_EQUAL -> compareNumeric(fieldValue, condition.value) >= 0
            ConditionOperator.IN -> condition.value.split(",").map { it.trim() }
                .any { fieldValue.equals(it, ignoreCase = true) }
            ConditionOperator.NOT_IN -> !condition.value.split(",").map { it.trim() }
                .any { fieldValue.equals(it, ignoreCase = true) }
            ConditionOperator.REGEX_MATCHES -> fieldValue.matches(Regex(condition.value))
            ConditionOperator.IS_EMPTY -> fieldValue.isBlank()
            ConditionOperator.IS_NOT_EMPTY -> fieldValue.isNotBlank()
        }
    }

    private suspend fun getFieldValue(
        transaction: TransactionEntity,
        smsText: String?,
        field: TransactionField
    ): String {
        return when (field) {
            TransactionField.AMOUNT -> transaction.amount.toString()
            TransactionField.TYPE -> transaction.transactionType.name
            TransactionField.CATEGORY -> transaction.category ?: ""
            TransactionField.MERCHANT -> transaction.merchantName
            TransactionField.NARRATION -> transaction.description ?: ""
            TransactionField.SMS_TEXT -> smsText ?: ""
            TransactionField.BANK_NAME -> {
                val account = transaction.accountId?.let { accountBalanceRepository.getAccountById(it) }
                account?.bankName ?: ""
            }
            TransactionField.SUBCATEGORY -> transaction.subcategory ?: ""
            TransactionField.ACCOUNT -> {
                val account = transaction.accountId?.let { accountBalanceRepository.getAccountById(it) }
                "${account?.bankName ?: ""}|${account?.accountLast4 ?: ""}"
            }
        }
    }

    private fun compareNumeric(value1: String, value2: String): Int {
        return try {
            BigDecimal(value1).compareTo(BigDecimal(value2))
        } catch (e: NumberFormatException) {
            value1.compareTo(value2)
        }
    }

    private suspend fun applyActions(
        transaction: TransactionEntity,
        actions: List<RuleAction>
    ): Pair<TransactionEntity, List<FieldModification>> {
        var modifiedTransaction = transaction
        val modifications = mutableListOf<FieldModification>()

        for (action in actions) {
            // Skip BLOCK actions as they don't modify the transaction
            if (action.actionType == ActionType.BLOCK) {
                continue
            }

            val oldValue = getFieldValue(modifiedTransaction, null, action.field)
            val (newTransaction, newValue) = applyAction(modifiedTransaction, action)

            if (oldValue != newValue) {
                modifiedTransaction = newTransaction
                modifications.add(
                    FieldModification(
                        field = action.field,
                        oldValue = oldValue.takeIf { it.isNotBlank() },
                        newValue = newValue,
                        actionType = action.actionType
                    )
                )
            }
        }

        return modifiedTransaction to modifications
    }

    private suspend fun applyAction(
        transaction: TransactionEntity,
        action: RuleAction
    ): Pair<TransactionEntity, String> {
        return when (action.field) {
            TransactionField.CATEGORY -> {
                val newValue = when (action.actionType) {
                    ActionType.SET -> action.value
                    ActionType.CLEAR -> ""
                    else -> transaction.category ?: ""
                }
                transaction.copy(category = newValue) to newValue
            }
            TransactionField.SUBCATEGORY -> {
                val newValue = when (action.actionType) {
                    ActionType.SET -> action.value
                    ActionType.CLEAR -> ""
                    else -> transaction.subcategory ?: ""
                }
                
                var modifiedTransaction = transaction.copy(subcategory = newValue)
                
                // If subcategory was set to a new value, automatically resolve and set the parent Category
                if (newValue.isNotBlank() && newValue != transaction.subcategory) {
                    val subcategory = subcategoryRepository.getSubcategoryByName(newValue)
                    if (subcategory != null) {
                        val category = categoryRepository.getCategoryById(subcategory.categoryId)
                        if (category != null) {
                            modifiedTransaction = modifiedTransaction.copy(category = category.name)
                        }
                    }
                }
                
                modifiedTransaction to newValue
            }
            TransactionField.MERCHANT -> {
                val newValue = when (action.actionType) {
                    ActionType.SET -> action.value
                    ActionType.APPEND -> "${transaction.merchantName}${action.value}"
                    ActionType.PREPEND -> "${action.value}${transaction.merchantName}"
                    ActionType.CLEAR -> ""
                    else -> transaction.merchantName
                }
                transaction.copy(merchantName = newValue) to newValue
            }
            TransactionField.NARRATION -> {
                val newValue = when (action.actionType) {
                    ActionType.SET -> action.value
                    ActionType.APPEND -> "${transaction.description ?: ""}${action.value}"
                    ActionType.PREPEND -> "${action.value}${transaction.description ?: ""}"
                    ActionType.CLEAR -> ""
                    else -> transaction.description ?: ""
                }
                transaction.copy(description = newValue) to newValue
            }
            TransactionField.TYPE -> {
                val newType = when (action.actionType) {
                    ActionType.SET -> {
                        // Convert string value to TransactionType enum
                        try {
                            TransactionType.valueOf(action.value.uppercase())
                        } catch (e: Exception) {
                            transaction.transactionType
                        }
                    }
                    else -> transaction.transactionType
                }
                transaction.copy(transactionType = newType) to newType.name
            }
            TransactionField.ACCOUNT -> {
                val parts = action.value.split("|")
                if (parts.size == 2) {
                    val bankName = parts[0]
                    val accountNumber = parts[1]
                    val account = accountBalanceRepository.getLatestBalance(bankName, accountNumber)
                    transaction.copy(accountId = account?.id?.toString()) to action.value
                } else {
                    transaction to action.value
                }
            }
            else -> transaction to getFieldValue(transaction, null, action.field)
        }
    }

    fun serializeConditions(conditions: List<RuleCondition>): String {
        return json.encodeToString(conditions)
    }

    fun deserializeConditions(conditionsJson: String): List<RuleCondition> {
        return try {
            json.decodeFromString(conditionsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeActions(actions: List<RuleAction>): String {
        return json.encodeToString(actions)
    }

    fun deserializeActions(actionsJson: String): List<RuleAction> {
        return try {
            json.decodeFromString(actionsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeFieldModifications(modifications: List<FieldModification>): String {
        return json.encodeToString(modifications)
    }

    fun deserializeFieldModifications(modificationsJson: String): List<FieldModification> {
        return try {
            json.decodeFromString(modificationsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
}