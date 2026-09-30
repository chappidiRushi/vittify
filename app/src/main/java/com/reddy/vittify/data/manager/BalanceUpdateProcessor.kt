package com.reddy.vittify.data.manager

import android.util.Log
import com.reddy.vittify.BuildConfig
import com.reddy.parser.core.ParsedTransaction
import com.reddy.vittify.data.database.entity.CardType
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.mapper.toEntityType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CardRepository
import com.reddy.vittify.domain.service.TransactionBalanceService
import com.reddy.vittify.utils.PiiRedactor
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceUpdateProcessor @Inject constructor(
    private val cardRepository: CardRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionBalanceService: TransactionBalanceService
) {
    companion object {
        private const val TAG = "BalanceUpdateProcessor"
    }

    suspend fun process(
        parsedTransaction: ParsedTransaction,
        entity: TransactionEntity,
        rowId: Long
    ) {
        // Fetch the account using entity.accountId
        val account = entity.accountId?.let { accountBalanceRepository.getAccountById(it) }
        val effectiveBankName = account?.bankName
        val effectiveAccountLast4 = account?.accountLast4

        // If entity has no accountId or no matching account, skip balance update
        if (account == null || effectiveBankName == null || effectiveAccountLast4 == null) {
            Log.d(TAG, "No existing account associated with transaction; skipping balance update and card/account creation.")
            return
        }

        // Detect whether a rule changed the account from what the parser originally extracted.
        val ruleChangedAccount = (effectiveBankName != parsedTransaction.bankName ||
                 effectiveAccountLast4 != parsedTransaction.accountLast4)

        val isFromCard = parsedTransaction.isFromCard && !ruleChangedAccount

        val targetAccountLast4: String? = if (isFromCard) {
            val card = parsedTransaction.bankName?.let { cardRepository.getCard(it, effectiveAccountLast4) }
                ?: cardRepository.findCard(effectiveAccountLast4)

            if (card == null) {
                Log.d(TAG, "Card not found for ${parsedTransaction.bankName}/$effectiveAccountLast4; skipping card balance update.")
                null
            } else {
                cardRepository.updateCardBalance(
                    cardId = card.id,
                    balance = parsedTransaction.balance,
                    source = sanitizeSmsSource(parsedTransaction).take(200),
                    date = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(parsedTransaction.timestamp),
                        ZoneId.systemDefault()
                    )
                )

                when {
                    card.cardType == CardType.CREDIT -> effectiveAccountLast4
                    card.cardType == CardType.DEBIT && card.accountLast4 != null -> card.accountLast4
                    else -> null
                }
            }
        } else {
            effectiveAccountLast4
        }

        if (targetAccountLast4 != null) {
            // Apply the balance update using the centralized service
            val transactionId = if (rowId != -1L) rowId else 0L

            val targetAccount = if (targetAccountLast4 == effectiveAccountLast4) {
                account
            } else {
                parsedTransaction.bankName?.let {
                    accountBalanceRepository.getLatestBalance(it, targetAccountLast4)
                }
            }

            val updatedEntity = if (targetAccount != null && targetAccount.id.toString() != entity.accountId) {
                entity.copy(accountId = targetAccount.id.toString())
            } else {
                entity
            }

            transactionBalanceService.applyTransaction(
                transaction = updatedEntity,
                transactionId = transactionId,
                explicitBalance = if (ruleChangedAccount) null else parsedTransaction.balance,
                creditLimit = if (ruleChangedAccount) null else parsedTransaction.creditLimit,
                smsSource = sanitizeSmsSource(parsedTransaction)
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Saved balance update for $effectiveBankName/$targetAccountLast4" +
                    if (ruleChangedAccount) " (rule-assigned account)" else "")
            }
        }
    }

    private fun sanitizeSmsSource(parsedTransaction: ParsedTransaction): String {
        return PiiRedactor.redact(parsedTransaction.smsBody).take(500)
    }
}
