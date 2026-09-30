package com.reddy.vittify.data.database.dao

import androidx.room.*
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime

@Dao
abstract class AccountBalanceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAccount(account: AccountBalanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertBalance(balance: AccountBalanceEntity): Long

    @Query("SELECT * FROM account_balances WHERE id = :id LIMIT 1")
    abstract suspend fun getAccountById(id: String): AccountBalanceEntity?

    @Query("SELECT * FROM account_balances WHERE id = :id LIMIT 1")
    abstract suspend fun getBalanceById(id: String): AccountBalanceEntity?

    @Query("""
        SELECT * FROM account_balances 
        WHERE bank_name = :bankName AND account_last4 = :accountLast4
        LIMIT 1
    """)
    abstract suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity?

    @Query("""
        SELECT DISTINCT account_last4 FROM account_balances
        WHERE bank_name = :bankName
        AND LENGTH(account_last4) >= 4
        AND account_last4 LIKE '%' || :suffix
    """)
    abstract suspend fun getAccountLast4sEndingWith(bankName: String, suffix: String): List<String>

    @Query("""
        SELECT * FROM account_balances
        WHERE bank_name = :bankName AND account_last4 = :accountLast4 AND timestamp <= :timestamp
        LIMIT 1
    """)
    abstract suspend fun getLatestBalanceOnOrBefore(
        bankName: String,
        accountLast4: String,
        timestamp: LocalDateTime
    ): AccountBalanceEntity?

    @Query("""
        SELECT * FROM account_balances 
        WHERE bank_name = :bankName AND account_last4 = :accountLast4
        LIMIT 1
    """)
    abstract fun getLatestBalanceFlow(bankName: String, accountLast4: String): Flow<AccountBalanceEntity?>

    @Query("SELECT * FROM account_balances ORDER BY balance DESC")
    abstract fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>>

    @Query("SELECT * FROM account_balances ORDER BY balance DESC")
    abstract fun getAllBalances(): Flow<List<AccountBalanceEntity>>

    @Query("DELETE FROM account_balances")
    abstract suspend fun deleteAllBalances()

    @Query("DELETE FROM account_balances WHERE is_sample = 1")
    abstract suspend fun deleteSampleBalances()

    @Query("SELECT * FROM account_balances ORDER BY balance DESC")
    abstract fun getCurrentMonthLatestBalances(): Flow<List<AccountBalanceEntity>>

    @Query("SELECT SUM(balance) FROM account_balances")
    abstract fun getTotalBalance(): Flow<BigDecimal?>

    @Query("""
        SELECT * FROM account_balances
        WHERE bank_name = :bankName AND account_last4 = :accountLast4
        AND timestamp BETWEEN :startDate AND :endDate
    """)
    abstract fun getBalanceHistory(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<AccountBalanceEntity>>

    @Query("SELECT COUNT(*) FROM account_balances")
    abstract fun getAccountCount(): Flow<Int>

    @Query("DELETE FROM account_balances WHERE timestamp < :beforeDate")
    abstract suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int

    @Update
    abstract suspend fun updateBalance(balance: AccountBalanceEntity)

    @Delete
    abstract suspend fun deleteBalance(balance: AccountBalanceEntity)

    @Query("""SELECT * FROM account_balances 
        WHERE bank_name = :bankName AND account_last4 = :accountLast4""")
    abstract suspend fun getBalanceHistoryForAccount(bankName: String, accountLast4: String): List<AccountBalanceEntity>

    @Query("DELETE FROM account_balances WHERE id = :id")
    abstract suspend fun deleteBalanceById(id: String)

    @Query("UPDATE account_balances SET balance = :newBalance WHERE id = :id")
    abstract suspend fun updateBalanceById(id: String, newBalance: BigDecimal)

    @Query("""SELECT COUNT(*) FROM account_balances
        WHERE bank_name = :bankName AND account_last4 = :accountLast4""")
    abstract suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int

    @Query("DELETE FROM account_balances WHERE bank_name = :bankName AND account_last4 = :accountLast4")
    abstract suspend fun deleteAccount(bankName: String, accountLast4: String): Int

    @Query("UPDATE account_balances SET bank_name = :newBankName WHERE bank_name = :oldBankName AND account_last4 = :accountLast4")
    abstract suspend fun updateAccountBankName(oldBankName: String, accountLast4: String, newBankName: String): Int

    @Query("UPDATE account_balances SET custom_id = :customId WHERE bank_name = :bankName AND account_last4 = :accountLast4")
    abstract suspend fun updateAccountCustomId(bankName: String, accountLast4: String, customId: String?): Int

    /** Finds the account record for a given last-4 digits, regardless of bank name. */
    @Query("""
        SELECT * FROM account_balances
        WHERE account_last4 = :accountLast4 OR account_last4 LIKE '%' || :accountLast4
        LIMIT 1
    """)
    abstract suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity?

    @Query("""
        SELECT * FROM account_balances
        WHERE bank_name = :bankName AND account_last4 = :accountLast4
        LIMIT 1
    """)
    abstract suspend fun getEarliestBalance(bankName: String, accountLast4: String): AccountBalanceEntity?

    @Query("DELETE FROM account_balances WHERE (owner_id != '' AND owner_id != :myDeviceId) AND (:partnerDeviceId = '' OR owner_id = :partnerDeviceId)")
    abstract suspend fun deletePartnerBalances(partnerDeviceId: String, myDeviceId: String): Int

    @Query("UPDATE account_balances SET owner_id = :newOwnerId WHERE owner_id IS NULL OR owner_id = ''")
    abstract suspend fun updateOwnerIdForBlank(newOwnerId: String): Int

    @Query("UPDATE account_balances SET owner_id = :newOwnerId WHERE id = :accountId")
    abstract suspend fun updateOwnerIdForAccount(accountId: String, newOwnerId: String): Int

    @Query("UPDATE account_balances SET owner_id = :newOwnerId WHERE owner_id = :oldOwnerId")
    abstract suspend fun updateOwnerIdForOwner(oldOwnerId: String, newOwnerId: String): Int
}
