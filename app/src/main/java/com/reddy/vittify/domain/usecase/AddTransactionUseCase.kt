package com.reddy.vittify.domain.usecase

import com.reddy.vittify.utils.SubscriptionUtils

import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.SubscriptionState
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.SubscriptionRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.domain.service.TransactionBalanceService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

class AddTransactionUseCase
@Inject
constructor(
        private val transactionRepository: TransactionRepository,
        private val subscriptionRepository: SubscriptionRepository,
        private val transactionBalanceService: TransactionBalanceService,
        private val accountBalanceRepository: AccountBalanceRepository
) {
    suspend fun execute(
            amount: BigDecimal,
            merchant: String,
            category: String,
            type: TransactionType,
            date: LocalDateTime,
            notes: String? = null,
            subcategory: String? = null,
            isRecurring: Boolean = false,
            bankName: String? = null,
            accountLast4: String? = null,
            currency: String = "INR",
            sourceAccountId: String? = null,
            targetAccountBankName: String? = null,
            targetAccountLast4: String? = null,
            billingCycle: String? = null,
            createSubscription: Boolean = true,
            attachments: String = "",
            targetAmount: BigDecimal? = null
    ): Long {
        val resolvedAccountId = sourceAccountId ?: run {
            if (bankName != null && accountLast4 != null) {
                val existing = accountBalanceRepository.getLatestBalance(bankName, accountLast4)
                existing?.id ?: run {
                    val isCredit = (type == TransactionType.CREDIT)
                    val newAccount = AccountBalanceEntity(
                        bankName = bankName,
                        accountLast4 = accountLast4,
                        balance = BigDecimal.ZERO,
                        timestamp = date,
                        isCreditCard = isCredit,
                        currency = currency
                    )
                    accountBalanceRepository.insertBalance(newAccount)
                }
            } else if (accountLast4 != null) {
                accountBalanceRepository.getAccountByLast4(accountLast4)?.id
            } else null
        }

        // Create the transaction entity
        val transaction =
                TransactionEntity(
                        accountId = resolvedAccountId,
                        amount = amount,
                        merchantName = merchant,
                        category = category,
                        subcategory = subcategory,
                        transactionType = type,
                        dateTime = date,
                        description = notes,
                        smsBody = null, // null indicates manual entry
                        smsSender = null, // null indicates manual entry
                        fromAccount = accountLast4,
                        toAccount = targetAccountLast4,
                        balanceAfter = null,
                        uuid = UUID.randomUUID().toString(),
                        isRecurring = isRecurring,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                        currency = currency,
                        billingCycle = billingCycle,
                        attachments = attachments,
                        targetAmount = targetAmount
                )

        // Insert the transaction
        val transactionId = transactionRepository.insertTransaction(transaction)

        // Apply balance effects via the centralized service
        transactionBalanceService.applyTransaction(
            transaction = transaction.copy(id = transactionId),
            transactionId = transactionId
        )

        // Create subscription if requested and recurring
        if (createSubscription && isRecurring) {
            val nextPaymentDate =
                    SubscriptionUtils.calculateNextPaymentDate(date.toLocalDate(), billingCycle)

            val subscription =
                    SubscriptionEntity(
                            merchantName = merchant,
                            amount = amount,
                            nextPaymentDate = nextPaymentDate,
                            state = SubscriptionState.ACTIVE,
                            bankName = bankName,
                            category = category,
                            subcategory = subcategory,
                            currency = currency,
                            billingCycle = billingCycle,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                    )

            subscriptionRepository.insertSubscription(subscription)
        }
        return transactionId
    }
}
