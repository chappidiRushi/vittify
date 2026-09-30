package com.reddy.vittify.data.backup

import com.reddy.vittify.data.database.entity.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Extension functions to sanitize entities deserialized from backup.
 * Gson's reflection-based deserialization can bypass Kotlin's null-safety,
 * resulting in null values for non-nullable fields if they were missing in the JSON.
 * These functions ensure all non-nullable fields have valid default values.
 */

fun CategoryEntity.sanitize(): CategoryEntity {
    val resolvedType = categoryType ?: if (isIncome) CategoryType.INCOME else CategoryType.EXPENSE
    return CategoryEntity(
        id = id,
        name = name ?: "",
        color = color ?: "#757575",
        iconResId = iconResId,
        iconName = iconName ?: "",
        description = description ?: "",
        isSystem = isSystem,
        isIncome = resolvedType == CategoryType.INCOME,
        categoryType = resolvedType,
        displayOrder = displayOrder,
        defaultName = defaultName,
        defaultColor = defaultColor,
        defaultIconResId = defaultIconResId,
        defaultIconName = defaultIconName,
        defaultDescription = defaultDescription,
        defaultCategoryType = defaultCategoryType,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now()
    )
}

fun SubcategoryEntity.sanitize(): SubcategoryEntity {
    return SubcategoryEntity(
        id = id,
        categoryId = categoryId,
        name = name ?: "",
        iconResId = iconResId,
        iconName = iconName ?: "",
        color = color ?: "#757575",
        isSystem = isSystem,
        defaultName = defaultName,
        defaultIconResId = defaultIconResId,
        defaultIconName = defaultIconName,
        defaultColor = defaultColor,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now()
    )
}

fun TransactionEntity.sanitize(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount ?: BigDecimal.ZERO,
        merchantName = merchantName ?: "",
        category = category ?: "",
        subcategory = subcategory,
        transactionType = transactionType ?: TransactionType.EXPENSE,
        dateTime = dateTime ?: LocalDateTime.now(),
        description = description,
        smsBody = smsBody,
        smsSender = smsSender,
        balanceAfter = balanceAfter,
        uuid = uuid ?: "",
        isRecurring = isRecurring,
        isDeleted = isDeleted,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now(),
        currency = currency ?: "INR",
        fromAccount = fromAccount,
        toAccount = toAccount,
        reference = reference,
        billingCycle = billingCycle,
        attachments = attachments ?: "",
        isSample = isSample,
        targetAmount = targetAmount,
        accountId = accountId,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        ownerId = ownerId ?: "",
        isDuplicateDismissed = isDuplicateDismissed
    )
}

fun CardEntity.sanitize(): CardEntity {
    return CardEntity(
        id = id,
        cardLast4 = cardLast4 ?: "",
        cardType = cardType ?: CardType.DEBIT,
        bankName = bankName ?: "",
        accountLast4 = accountLast4,
        nickname = nickname,
        isActive = isActive,
        lastBalance = lastBalance,
        lastBalanceSource = lastBalanceSource,
        lastBalanceDate = lastBalanceDate,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now(),
        currency = currency ?: "INR",
        isSample = isSample,
        ownerId = ownerId ?: ""
    )
}

fun SubscriptionEntity.sanitize(): SubscriptionEntity {
    return SubscriptionEntity(
        id = id,
        merchantName = merchantName ?: "",
        amount = amount ?: BigDecimal.ZERO,
        nextPaymentDate = nextPaymentDate,
        state = state ?: SubscriptionState.ACTIVE,
        bankName = bankName,
        umn = umn,
        category = category,
        subcategory = subcategory,
        smsBody = smsBody,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now(),
        currency = currency ?: "INR",
        billingCycle = billingCycle,
        lastPaidDate = lastPaidDate,
        isSample = isSample,
        ownerId = ownerId ?: ""
    )
}

fun BudgetEntity.sanitize(): BudgetEntity {
    return BudgetEntity(
        id = id,
        name = name ?: "",
        amount = amount ?: BigDecimal.ZERO,
        year = year,
        month = month,
        currency = currency ?: "INR",
        isActive = isActive,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now(),
        startDate = startDate ?: LocalDateTime.now(),
        endDate = endDate ?: (startDate ?: LocalDateTime.now()).plusMonths(1),
        periodType = periodType ?: BudgetPeriod.MONTHLY,
        trackType = trackType ?: BudgetTrackType.ALL_TRANSACTIONS,
        budgetType = budgetType ?: BudgetType.EXPENSE,
        accountIds = accountIds ?: emptyList(),
        color = color ?: "#4CAF50",
        isSample = isSample,
        ownerId = ownerId ?: ""
    )
}

fun AccountBalanceEntity.sanitize(): AccountBalanceEntity {
    val validId = if (id.isNullOrBlank() || id == "0") com.reddy.vittify.utils.NanoId.generate() else id
    return AccountBalanceEntity(
        id = validId,
        bankName = bankName ?: "",
        accountLast4 = accountLast4 ?: "",
        accountName = accountName,
        balance = balance ?: BigDecimal.ZERO,
        currency = currency ?: "INR",
        isCreditCard = isCreditCard,
        isWallet = isWallet,
        creditLimit = creditLimit,
        iconResId = iconResId,
        iconName = iconName ?: "",
        color = color ?: "#33B5E5",
        isSample = isSample,
        customId = customId,
        timestamp = timestamp ?: LocalDateTime.now(),
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now(),
        ownerId = ownerId ?: ""
    )
}

fun TransactionItemEntity.sanitize(): TransactionItemEntity {
    return TransactionItemEntity(
        id = id,
        transactionId = transactionId,
        name = name ?: "",
        amount = amount ?: BigDecimal.ZERO,
        category = category ?: "",
        subcategory = subcategory
    )
}

fun UnrecognizedSmsEntity.sanitize(): UnrecognizedSmsEntity {
    return UnrecognizedSmsEntity(
        id = id,
        sender = sender ?: "",
        smsBody = smsBody ?: "",
        receivedAt = receivedAt ?: LocalDateTime.now(),
        reported = reported,
        isDeleted = isDeleted,
        createdAt = createdAt ?: LocalDateTime.now()
    )
}

fun MerchantMappingEntity.sanitize(): MerchantMappingEntity {
    return MerchantMappingEntity(
        merchantName = merchantName ?: "",
        category = category ?: "",
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now()
    )
}

fun ExchangeRateEntity.sanitize(): ExchangeRateEntity {
    return ExchangeRateEntity(
        id = id,
        fromCurrency = fromCurrency ?: "USD",
        toCurrency = toCurrency ?: "INR",
        rate = rate ?: BigDecimal.ONE,
        provider = provider ?: "default",
        updatedAt = updatedAt ?: LocalDateTime.now(),
        updatedAtUnix = updatedAtUnix,
        expiresAt = expiresAt ?: LocalDateTime.now().plusDays(1),
        expiresAtUnix = expiresAtUnix,
        isCustom = isCustom
    )
}

fun RuleEntity.sanitize(): RuleEntity {
    return RuleEntity(
        id = id ?: com.reddy.vittify.utils.NanoId.generate(),
        name = name ?: "",
        description = description,
        priority = priority,
        conditions = conditions ?: "[]",
        actions = actions ?: "[]",
        isActive = isActive,
        isSystemTemplate = isSystemTemplate,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now()
    )
}

fun RuleApplicationEntity.sanitize(): RuleApplicationEntity {
    return RuleApplicationEntity(
        id = id ?: com.reddy.vittify.utils.NanoId.generate(),
        ruleId = ruleId ?: "",
        ruleName = ruleName ?: "",
        transactionId = transactionId ?: "",
        fieldsModified = fieldsModified ?: "[]",
        appliedAt = appliedAt ?: LocalDateTime.now()
    )
}
