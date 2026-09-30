package com.reddy.vittify.data.database.entity

import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.reddy.vittify.R
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["account_id"])
    ]
)
data class TransactionEntity(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
        @ColumnInfo(name = "amount") val amount: BigDecimal,
        @ColumnInfo(name = "merchant_name") val merchantName: String,
        @ColumnInfo(name = "category") val category: String,
        @ColumnInfo(name = "subcategory") val subcategory: String? = null,
        @ColumnInfo(name = "transaction_type") val transactionType: TransactionType,
        @ColumnInfo(name = "date_time") val dateTime: LocalDateTime,
        @ColumnInfo(name = "description") val description: String? = null,
        @ColumnInfo(name = "sms_body") val smsBody: String? = null,
        @ColumnInfo(name = "sms_sender") val smsSender: String? = null,
        @ColumnInfo(name = "balance_after") val balanceAfter: BigDecimal? = null,
        @ColumnInfo(name = "uuid", defaultValue = "") val uuid: String = "",
        @ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
        @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
        @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
        @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now(),
        @ColumnInfo(name = "currency", defaultValue = "INR") val currency: String = "INR",
        @SerializedName(value = "fromAccount", alternate = ["from_account", "accountNumber", "account_number"])
        @ColumnInfo(name = "from_account") val fromAccount: String? = null,
        @SerializedName(value = "toAccount", alternate = ["to_account", "targetAccount", "target_account"])
        @ColumnInfo(name = "to_account") val toAccount: String? = null,
        @ColumnInfo(name = "reference") val reference: String? = null,
        @ColumnInfo(name = "billing_cycle") val billingCycle: String? = null,
        @ColumnInfo(name = "attachments", defaultValue = "") val attachments: String = "",
        @ColumnInfo(name = "is_sample", defaultValue = "0") val isSample: Boolean = false,
        @ColumnInfo(name = "target_amount") val targetAmount: BigDecimal? = null,
        @SerializedName(value = "accountId", alternate = ["account_id"])
        @ColumnInfo(name = "account_id") val accountId: String? = null,
        @SerializedName(value = "fromAccountId", alternate = ["from_account_id"])
        @ColumnInfo(name = "from_account_id") val fromAccountId: String? = null,
        @SerializedName(value = "toAccountId", alternate = ["to_account_id"])
        @ColumnInfo(name = "to_account_id") val toAccountId: String? = null,
        @SerializedName(value = "ownerId", alternate = ["owner_id"])
        @ColumnInfo(name = "owner_id", defaultValue = "") val ownerId: String = "",
        @SerializedName(value = "isDuplicateDismissed", alternate = ["is_duplicate_dismissed"])
        @ColumnInfo(name = "is_duplicate_dismissed", defaultValue = "0") val isDuplicateDismissed: Boolean = false
)

enum class TransactionType(@StringRes val labelRes: Int) {
    INCOME(R.string.type_income),
    EXPENSE(R.string.type_expense),
    CREDIT(R.string.type_credit),
    TRANSFER(R.string.type_transfer),
    INVESTMENT(R.string.type_investment),
    BALANCE_UPDATE(R.string.type_balance_update)
}
