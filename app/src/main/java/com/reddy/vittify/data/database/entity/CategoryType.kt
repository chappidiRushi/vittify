package com.reddy.vittify.data.database.entity

import androidx.annotation.StringRes
import com.reddy.vittify.R

enum class CategoryType(@StringRes val labelRes: Int) {
    INCOME(R.string.type_income),
    EXPENSE(R.string.type_expense),
    CREDIT(R.string.type_credit),
    INVESTMENT(R.string.type_investment);

    companion object {
        fun fromString(value: String?): CategoryType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: EXPENSE
        }

        fun fromTransactionType(transactionType: TransactionType): CategoryType? {
            return when (transactionType) {
                TransactionType.INCOME -> INCOME
                TransactionType.EXPENSE -> EXPENSE
                TransactionType.CREDIT -> CREDIT
                TransactionType.INVESTMENT -> INVESTMENT
                TransactionType.TRANSFER -> null
                TransactionType.BALANCE_UPDATE -> null
            }
        }
    }
}

