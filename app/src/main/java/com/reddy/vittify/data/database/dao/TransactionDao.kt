package com.reddy.vittify.data.database.dao

import androidx.room.*
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY date_time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date_time DESC")
    fun getAllTransactionsIncludingDeleted(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE is_deleted = 0")
    fun getTransactionCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): TransactionEntity?

    @Query(
            """
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND date_time BETWEEN :startDate AND :endDate
        ORDER BY date_time DESC
    """
    )
    fun getTransactionsBetweenDates(
            startDate: LocalDateTime,
            endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>

    /**
     * Optimized query that filters transactions at the database level. Combines date range,
     * currency, and transaction type filters to reduce memory usage.
     *
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @param currency Currency code to filter by (e.g., "INR", "USD")
     * @param transactionType Optional transaction type filter (null means all types)
     * @return Flow of filtered transactions ordered by date descending
     */
    @Query(
            """
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND date_time BETWEEN :startDate AND :endDate
        AND currency = :currency
        AND (:transactionType IS NULL OR transaction_type = :transactionType)
        ORDER BY date_time DESC
    """
    )
    fun getTransactionsFiltered(
            startDate: LocalDateTime,
            endDate: LocalDateTime,
            currency: String,
            transactionType: TransactionType?
    ): Flow<List<TransactionEntity>>

    @Query(
            """
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND transaction_type = :type 
        ORDER BY date_time DESC
    """
    )
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query(
            """
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND category = :category 
        ORDER BY date_time DESC
    """
    )
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query(
            """
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND (merchant_name LIKE '%' || :searchQuery || '%' 
        OR description LIKE '%' || :searchQuery || '%'
        OR sms_body LIKE '%' || :searchQuery || '%') 
        ORDER BY date_time DESC
    """
    )
    fun searchTransactions(searchQuery: String): Flow<List<TransactionEntity>>

    @Query(
            """
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND (merchant_name LIKE '%' || :searchQuery || '%' 
        OR description LIKE '%' || :searchQuery || '%'
        OR sms_body LIKE '%' || :searchQuery || '%') 
        ORDER BY date_time DESC
        LIMIT 20
    """
    )
    suspend fun searchTransactionsList(searchQuery: String): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND (
            merchant_name LIKE '%' || :query || '%' 
            OR (description IS NOT NULL AND description LIKE '%' || :query || '%')
            OR category LIKE '%' || :query || '%'
            OR (subcategory IS NOT NULL AND subcategory LIKE '%' || :query || '%')
            OR amount LIKE '%' || :query || '%'
            OR CAST(amount AS TEXT) LIKE '%' || :query || '%'
        ) 
        ORDER BY date_time DESC
        LIMIT :limit
    """
    )
    suspend fun searchTransactionsForGlobal(query: String, limit: Int = 20): List<TransactionEntity>

    @Query("SELECT DISTINCT category FROM transactions WHERE is_deleted = 0 ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query(
            """
        SELECT category FROM transactions
        WHERE is_deleted = 0
        GROUP BY category
        ORDER BY COUNT(*) DESC
        LIMIT :limit
    """
    )
    suspend fun getTopCategoriesByUsage(limit: Int): List<String>

    @Query(
            "SELECT DISTINCT merchant_name FROM transactions WHERE is_deleted = 0 ORDER BY merchant_name ASC"
    )
    fun getAllMerchants(): Flow<List<String>>

    @Query(
            """
        SELECT SUM(amount) FROM transactions 
        WHERE is_deleted = 0 
        AND transaction_type = :type 
        AND date_time BETWEEN :startDate AND :endDate
    """
    )
    suspend fun getTotalAmountByTypeAndPeriod(
            type: TransactionType,
            startDate: LocalDateTime,
            endDate: LocalDateTime
    ): Double?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("DELETE FROM transactions") suspend fun deleteAllTransactions()
    
    @Query("DELETE FROM transactions WHERE is_sample = 1")
    suspend fun deleteSampleTransactions()

    @Query("UPDATE transactions SET category = :newCategory WHERE merchant_name = :merchantName")
    suspend fun updateCategoryForMerchant(merchantName: String, newCategory: String)

    /** Updates category and subcategory for all non-deleted transactions whose merchant name CONTAINS the keyword */
    @Query("UPDATE transactions SET category = :newCategory, subcategory = :newSubcategory WHERE is_deleted = 0 AND merchant_name LIKE '%' || :merchantName || '%'")
    suspend fun updateCategoryAndSubcategoryForMerchantContains(merchantName: String, newCategory: String, newSubcategory: String?)

    @Query(
            "SELECT COUNT(*) FROM transactions WHERE merchant_name = :merchantName AND id != :excludeId"
    )
    suspend fun getTransactionCountForMerchant(merchantName: String, excludeId: Long): Int

    /** Returns all non-deleted transactions whose merchant name CONTAINS the keyword, excluding one id */
    @Query(
        """
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND merchant_name LIKE '%' || :merchantName || '%'
        AND id != :excludeId
        ORDER BY date_time DESC
        """
    )
    suspend fun getTransactionsByMerchantContains(
        merchantName: String,
        excludeId: Long
    ): List<TransactionEntity>

    @Query("SELECT DISTINCT currency FROM transactions WHERE is_deleted = 0 ORDER BY currency")
    fun getAllCurrencies(): Flow<List<String>>

    @Query(
            "SELECT DISTINCT currency FROM transactions WHERE is_deleted = 0 AND date_time BETWEEN :startDate AND :endDate ORDER BY currency"
    )
    fun getCurrenciesForPeriod(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<String>>

    // Soft delete methods
    @Query("UPDATE transactions SET is_deleted = 1, updated_at = :updatedAt WHERE id = :transactionId")
    suspend fun softDeleteTransaction(transactionId: Long, updatedAt: LocalDateTime = LocalDateTime.now())

    // Method to check if transaction exists by SMS body (including deleted)
    @Query("SELECT * FROM transactions WHERE sms_body = :smsBody LIMIT 1")
    suspend fun getTransactionBySmsBody(smsBody: String): TransactionEntity?

    @Query("UPDATE transactions SET is_deleted = 1, updated_at = :updatedAt WHERE id IN (:transactionIds)")
    suspend fun softDeleteTransactions(transactionIds: List<Long>, updatedAt: LocalDateTime = LocalDateTime.now())

    @Query("DELETE FROM transactions WHERE id IN (:transactionIds)")
    suspend fun deleteTransactionsByIds(transactionIds: List<Long>)

    // Archived transactions queries
    @Query("SELECT * FROM transactions WHERE is_deleted = 1 ORDER BY updated_at DESC, date_time DESC")
    fun getArchivedTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE is_deleted = 1")
    fun getArchivedTransactionsCount(): Flow<Int>

    @Query("UPDATE transactions SET is_deleted = 0, updated_at = :updatedAt WHERE id = :transactionId")
    suspend fun restoreTransaction(transactionId: Long, updatedAt: LocalDateTime = LocalDateTime.now())

    @Query("UPDATE transactions SET is_deleted = 0, updated_at = :updatedAt WHERE id IN (:transactionIds)")
    suspend fun restoreTransactions(transactionIds: List<Long>, updatedAt: LocalDateTime = LocalDateTime.now())

    @Query("DELETE FROM transactions WHERE is_deleted = 1")
    suspend fun deleteAllArchivedTransactions(): Int

    @Query("DELETE FROM transactions WHERE is_deleted = 1 AND updated_at < :cutoffDate")
    suspend fun deleteArchivedTransactionsOlderThan(cutoffDate: LocalDateTime): Int

    @Query("SELECT * FROM transactions WHERE uuid = :uuid LIMIT 1")
    suspend fun getTransactionByUuid(uuid: String): TransactionEntity?

    @Query(
            """
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND date_time BETWEEN :startDate AND :endDate 
        ORDER BY date_time DESC
    """
    )
    suspend fun getTransactionsBetweenDatesList(
            startDate: LocalDateTime,
            endDate: LocalDateTime
    ): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND (
            account_id = :accountId
            OR from_account_id = :accountId
            OR to_account_id = :accountId
            OR (transaction_type = 'TRANSFER' AND (
                to_account = :accountLast4 OR to_account LIKE '%' || :accountLast4
                OR from_account = :accountLast4 OR from_account LIKE '%' || :accountLast4
            ))
        )
        ORDER BY date_time DESC
    """
    )
    fun getTransactionsByAccountId(
        accountId: String,
        accountLast4: String
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT t.* FROM transactions t
        LEFT JOIN account_balances a ON t.account_id = a.id
        WHERE t.is_deleted = 0
        AND (
            (a.bank_name = :bankName AND a.account_last4 = :accountLast4)
            OR
            (t.transaction_type = 'TRANSFER' AND (
                t.to_account = :accountLast4 OR t.to_account LIKE '%' || :accountLast4
                OR t.from_account = :accountLast4 OR t.from_account LIKE '%' || :accountLast4
            ))
        )
        ORDER BY t.date_time DESC
    """
    )
    fun getTransactionsByAccount(
        bankName: String,
        accountLast4: String
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND date_time BETWEEN :startDate AND :endDate
        AND (
            account_id = :accountId
            OR from_account_id = :accountId
            OR to_account_id = :accountId
            OR (transaction_type = 'TRANSFER' AND (
                to_account = :accountLast4 OR to_account LIKE '%' || :accountLast4
                OR from_account = :accountLast4 OR from_account LIKE '%' || :accountLast4
            ))
        )
        ORDER BY date_time DESC
    """
    )
    fun getTransactionsByAccountIdAndDateRange(
        accountId: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT t.* FROM transactions t
        LEFT JOIN account_balances a ON t.account_id = a.id
        WHERE t.is_deleted = 0 
        AND t.date_time BETWEEN :startDate AND :endDate
        AND (
            (a.bank_name = :bankName AND a.account_last4 = :accountLast4)
            OR
            (t.transaction_type = 'TRANSFER' AND (
                t.to_account = :accountLast4 OR t.to_account LIKE '%' || :accountLast4
                OR t.from_account = :accountLast4 OR t.from_account LIKE '%' || :accountLast4
            ))
        )
        ORDER BY t.date_time DESC
    """
    )
    fun getTransactionsByAccountAndDateRange(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        UPDATE transactions SET 
            account_id = CASE WHEN account_id = :oldAccountId THEN :newAccountId ELSE account_id END,
            from_account_id = CASE WHEN from_account_id = :oldAccountId THEN :newAccountId ELSE from_account_id END,
            to_account_id = CASE WHEN to_account_id = :oldAccountId THEN :newAccountId ELSE to_account_id END
        WHERE account_id = :oldAccountId OR from_account_id = :oldAccountId OR to_account_id = :oldAccountId
        """
    )
    suspend fun updateAccountIdForTransactions(oldAccountId: String, newAccountId: String)

    @Query("UPDATE transactions SET category = :newCategory, subcategory = :newSubcategory WHERE category = :oldCategory")
    suspend fun updateTransactionsCategory(oldCategory: String, newCategory: String, newSubcategory: String?)

    @Query("SELECT COUNT(*) FROM transactions WHERE is_deleted = 0 AND category = :category")
    suspend fun getTransactionCountByCategory(category: String): Int

    @Query(
        """
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND reference = :reference
        AND amount = :amount
        AND (:accountId IS NULL OR account_id = :accountId)
        AND date_time BETWEEN :startDate AND :endDate
        ORDER BY date_time DESC
        """
    )
    suspend fun getTransactionsByReferenceAndAmount(
        reference: String,
        amount: java.math.BigDecimal,
        accountId: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND date_time BETWEEN :startDate AND :endDate
    """
    )
    suspend fun findPotentialDuplicates(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE updated_at > :updatedAfter
        AND updated_at <= :updatedBefore
        AND currency = :currency
        AND is_sample = 0
        ORDER BY updated_at ASC, id ASC
        """
    )
    suspend fun getTransactionsUpdatedBetween(
        updatedAfter: LocalDateTime,
        updatedBefore: LocalDateTime,
        currency: String
    ): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND is_sample = 0
        AND currency = :currency
        AND date_time BETWEEN :startDate AND :endDate
        ORDER BY date_time DESC
        """
    )
    suspend fun getTransactionsBetweenDatesByCurrency(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        currency: String
    ): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE (owner_id != '' AND owner_id != :myDeviceId) AND (:partnerDeviceId = '' OR owner_id = :partnerDeviceId)")
    suspend fun deletePartnerTransactions(partnerDeviceId: String, myDeviceId: String): Int

    @Query(
        """
        SELECT category, subcategory FROM transactions
        WHERE is_deleted = 0 AND subcategory IS NOT NULL AND subcategory != ''
        GROUP BY category, subcategory
        ORDER BY MAX(date_time) DESC
        LIMIT :limit
        """
    )
    fun getRecentlyUsedSubcategories(limit: Int): Flow<List<RecentSubcategoryInfo>>

    @Query("UPDATE transactions SET owner_id = :newOwnerId WHERE owner_id IS NULL OR owner_id = ''")
    suspend fun updateOwnerIdForBlank(newOwnerId: String): Int

    @Query("UPDATE transactions SET owner_id = :newOwnerId WHERE id IN (:transactionIds)")
    suspend fun updateOwnerIdForTransactions(transactionIds: List<Long>, newOwnerId: String): Int

    @Query("UPDATE transactions SET owner_id = :newOwnerId WHERE owner_id = :oldOwnerId")
    suspend fun updateOwnerIdForOwner(oldOwnerId: String, newOwnerId: String): Int

    @Query("""
        UPDATE transactions 
        SET owner_id = (SELECT a.owner_id FROM account_balances a WHERE a.id = transactions.account_id LIMIT 1)
        WHERE account_id IS NOT NULL 
          AND account_id != '' 
          AND EXISTS (SELECT 1 FROM account_balances a WHERE a.id = transactions.account_id AND a.owner_id != '' AND a.owner_id != transactions.owner_id)
    """)
    suspend fun alignTransactionOwnersWithAccounts(): Int

    @Query("DELETE FROM transactions WHERE amount <= 0 AND is_deleted = 0")
    suspend fun deleteZeroAmountTransactions(): Int

    @Query("UPDATE transactions SET account_id = NULL, from_account_id = NULL, to_account_id = NULL WHERE id IN (:transactionIds)")
    suspend fun unlinkTransactionsByIds(transactionIds: List<Long>): Int

    @Query("UPDATE transactions SET account_id = NULL, from_account_id = NULL, to_account_id = NULL WHERE account_id = :accountId OR from_account_id = :accountId OR to_account_id = :accountId")
    suspend fun unlinkTransactionsForAccount(accountId: String): Int

    @Query("UPDATE transactions SET merchant_name = :merchantName WHERE id = :transactionId")
    suspend fun updateMerchantName(transactionId: Long, merchantName: String): Int

    @Query("UPDATE transactions SET is_duplicate_dismissed = :isDismissed WHERE id = :transactionId")
    suspend fun updateDuplicateDismissed(transactionId: Long, isDismissed: Boolean = true): Int

    @Query("UPDATE transactions SET is_duplicate_dismissed = :isDismissed WHERE id IN (:transactionIds)")
    suspend fun updateDuplicatesDismissed(transactionIds: List<Long>, isDismissed: Boolean = true): Int
}



