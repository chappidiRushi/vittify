package com.reddy.vittify.data.database.dao

import androidx.room.*
import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.CardType
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long
    
    @Update
    suspend fun updateCard(card: CardEntity)
    
    @Delete
    suspend fun deleteCard(card: CardEntity)
    
    @Query("SELECT * FROM cards WHERE bank_name = :bankName AND card_last4 = :cardLast4 LIMIT 1")
    suspend fun getCard(bankName: String, cardLast4: String): CardEntity?
    
    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    suspend fun getCardById(cardId: Long): CardEntity?
    
    @Query("SELECT * FROM cards WHERE card_last4 = :cardLast4")
    suspend fun getCardsByLast4(cardLast4: String): List<CardEntity>
    
    @Query("SELECT * FROM cards WHERE account_last4 = :accountLast4 AND is_active = 1")
    suspend fun getCardsForAccount(accountLast4: String): List<CardEntity>
    
    @Query("SELECT * FROM cards WHERE account_last4 = :accountLast4 AND is_active = 1")
    fun getCardsForAccountFlow(accountLast4: String): Flow<List<CardEntity>>
    
    @Query("""
        SELECT * FROM cards 
        WHERE account_last4 IS NULL 
        AND card_type = :cardType 
        AND is_active = 1
    """)
    suspend fun getOrphanedCards(cardType: CardType): List<CardEntity>
    
    @Query("SELECT * FROM cards WHERE is_active = 1 ORDER BY bank_name, card_last4")
    fun getAllActiveCards(): Flow<List<CardEntity>>
    
    @Query("SELECT * FROM cards ORDER BY bank_name, card_last4")
    fun getAllCards(): Flow<List<CardEntity>>
    
    @Query("""
        UPDATE cards 
        SET account_last4 = :accountLast4
        WHERE id = :cardId
    """)
    suspend fun linkCardToAccount(cardId: Long, accountLast4: String?)
    
    @Query("""
        UPDATE cards 
        SET is_active = :isActive
        WHERE id = :cardId
    """)
    suspend fun setCardActive(cardId: Long, isActive: Boolean)
    
    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int
    
    @Query("SELECT COUNT(*) FROM cards WHERE card_type = :cardType")
    suspend fun getCardCountByType(cardType: CardType): Int
    
    @Query("""
        SELECT * FROM cards 
        WHERE bank_name = :bankName 
        AND card_type = :cardType 
        AND is_active = 1
    """)
    suspend fun getBankCards(bankName: String, cardType: CardType): List<CardEntity>
    
    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("DELETE FROM cards WHERE is_sample = 1")
    suspend fun deleteSampleCards()

    @Query("DELETE FROM cards WHERE (owner_id != '' AND owner_id != :myDeviceId) AND (:partnerDeviceId = '' OR owner_id = :partnerDeviceId)")
    suspend fun deletePartnerCards(partnerDeviceId: String, myDeviceId: String): Int

    @Query("UPDATE cards SET owner_id = :newOwnerId WHERE owner_id IS NULL OR owner_id = ''")
    suspend fun updateOwnerIdForBlank(newOwnerId: String): Int

    @Query("UPDATE cards SET owner_id = :newOwnerId WHERE id = :cardId")
    suspend fun updateOwnerIdForCard(cardId: Long, newOwnerId: String): Int

    @Query("UPDATE cards SET owner_id = :newOwnerId WHERE owner_id = :oldOwnerId")
    suspend fun updateOwnerIdForOwner(oldOwnerId: String, newOwnerId: String): Int

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long): Int
}