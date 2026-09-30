package com.reddy.vittify.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.reddy.vittify.utils.NanoId
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(
    tableName = "account_balances",
    indices = [
        Index(value = ["bank_name", "account_last4"], unique = true)
    ]
)
data class AccountBalanceEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = NanoId.generate(),
    @ColumnInfo(name = "bank_name") val bankName: String,
    @ColumnInfo(name = "account_last4") val accountLast4: String,
    @ColumnInfo(name = "account_name") val accountName: String? = null,
    @ColumnInfo(name = "balance") val balance: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "currency", defaultValue = "INR") val currency: String = "INR",
    @ColumnInfo(name = "is_credit_card", defaultValue = "0") val isCreditCard: Boolean = false,
    @ColumnInfo(name = "is_wallet", defaultValue = "0") val isWallet: Boolean = false,
    @ColumnInfo(name = "credit_limit") val creditLimit: BigDecimal? = null,
    @ColumnInfo(name = "icon_res_id", defaultValue = "0") val iconResId: Int = 0,
    @ColumnInfo(name = "icon_name", defaultValue = "") val iconName: String = "",
    @ColumnInfo(name = "color", defaultValue = "#33B5E5") val color: String = "#33B5E5",
    @ColumnInfo(name = "is_sample", defaultValue = "0") val isSample: Boolean = false,
    @ColumnInfo(name = "custom_id") val customId: String? = null,
    @ColumnInfo(name = "timestamp") val timestamp: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "owner_id", defaultValue = "") val ownerId: String = ""
)
