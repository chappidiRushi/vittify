package com.reddy.vittify.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reddy.vittify.data.database.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionItemDao {

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId")
    fun getItemsForTransaction(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId")
    suspend fun getItemsForTransactionSync(transactionId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transaction_id IN (:transactionIds)")
    suspend fun getItemsForTransactions(transactionIds: List<Long>): List<TransactionItemEntity>

    @Query("SELECT DISTINCT transaction_id FROM transaction_items WHERE transaction_id IN (:transactionIds)")
    suspend fun getSplitTransactionIds(transactionIds: List<Long>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TransactionItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TransactionItemEntity>)

    @Update
    suspend fun updateItem(item: TransactionItemEntity)

    @Delete
    suspend fun deleteItem(item: TransactionItemEntity)

    @Query("DELETE FROM transaction_items WHERE transaction_id = :transactionId")
    suspend fun deleteItemsForTransaction(transactionId: Long)

    @Query("SELECT * FROM transaction_items")
    fun getAllItems(): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items")
    suspend fun getAllItemsSync(): List<TransactionItemEntity>

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAllItems()
}
