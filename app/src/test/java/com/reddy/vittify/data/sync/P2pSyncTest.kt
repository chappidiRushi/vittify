package com.reddy.vittify.data.sync

import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.CardType
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.sync.model.HandshakePayload
import com.reddy.vittify.data.sync.model.P2pPacket
import com.reddy.vittify.data.sync.model.SyncChangeEntity
import com.reddy.vittify.data.sync.model.SyncChangePayload
import com.reddy.vittify.data.sync.security.P2pCryptoEngine
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class P2pSyncTest {

    private val cryptoEngine = P2pCryptoEngine()
    private val serializer = SyncJsonSerializer()

    @Test
    fun testP2pCryptoEncryptionAndDecryptionRoundtrip() {
        val secret = "vittify-sync-test-passphrase-8842"
        val key = cryptoEngine.deriveKey(secret)

        val sampleText = "Paid 450.00 at Starbucks on 2026-09-09"
        val sampleBytes = sampleText.toByteArray(Charsets.UTF_8)

        val encrypted = cryptoEngine.encrypt(sampleBytes, key)
        assertNotNull(encrypted)
        assertTrue("Ciphertext should contain 12-byte IV + ciphertext", encrypted.size > 12)

        val decrypted = cryptoEngine.decrypt(encrypted, key)
        assertEquals(sampleText, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun testDecryptionFailsWithWrongKey() {
        val key1 = cryptoEngine.deriveKey("cluster-alpha-1234")
        val key2 = cryptoEngine.deriveKey("cluster-beta-5678")

        val plaintext = "Confidential financial transaction".toByteArray(Charsets.UTF_8)
        val encrypted = cryptoEngine.encrypt(plaintext, key1)

        try {
            cryptoEngine.decrypt(encrypted, key2)
            fail("Decryption with wrong key should throw AEADBadTagException or AEAD error")
        } catch (e: Exception) {
            // Expected
            assertTrue(e is javax.crypto.AEADBadTagException || e.cause is javax.crypto.AEADBadTagException)
        }
    }

    @Test
    fun testKeyExportAndImportBase64() {
        val key = cryptoEngine.deriveKey("test-cluster-key")
        val base64 = cryptoEngine.keyToBase64(key)
        assertNotNull(base64)
        assertTrue(base64.isNotBlank())

        val restoredKey = cryptoEngine.keyFromBase64(base64)
        assertEquals(key.algorithm, restoredKey.algorithm)
        assertArrayEquals(key.encoded, restoredKey.encoded)
    }

    @Test
    fun testPacketSerializationAndDeserialization() {
        val packet = P2pPacket(
            type = P2pPacket.TYPE_HANDSHAKE,
            senderDeviceId = "device-pixel-8",
            senderDeviceName = "Pixel 8",
            timestamp = 1725890000000L,
            handshake = HandshakePayload(
                lastSyncTimestamp = 1725800000000L,
                schemaVersion = 63
            )
        )

        val json = serializer.toJson(packet)
        val restored = serializer.fromJson(json, P2pPacket::class.java)

        assertEquals(packet.type, restored.type)
        assertEquals(packet.senderDeviceId, restored.senderDeviceId)
        assertEquals(packet.senderDeviceName, restored.senderDeviceName)
        assertEquals(packet.handshake?.lastSyncTimestamp, restored.handshake?.lastSyncTimestamp)
        assertEquals(packet.handshake?.schemaVersion, restored.handshake?.schemaVersion)
    }

    @Test
    fun testTransactionEntitySyncPayloadSerialization() {
        val txn = TransactionEntity(
            id = 55L,
            amount = BigDecimal("99.99"),
            merchantName = "Whole Foods",
            category = "Groceries",
            subcategory = "Produce",
            transactionType = TransactionType.EXPENSE,
            dateTime = LocalDateTime.of(2026, 9, 9, 14, 30, 0),
            description = "Apples & Bananas",
            smsBody = "Debited INR 99.99 at Whole Foods",
            uuid = "uuid-wf-99",
            isDeleted = false,
            createdAt = LocalDateTime.of(2026, 9, 9, 14, 31, 0),
            updatedAt = LocalDateTime.of(2026, 9, 9, 14, 35, 0)
        )

        val payloadJson = serializer.toJson(txn)
        val changePayload = SyncChangePayload(
            entityType = SyncChangeEntity.TYPE_TRANSACTION,
            entityId = txn.uuid,
            operation = SyncChangeEntity.OP_INSERT,
            payloadJson = payloadJson,
            timestamp = 1725892500000L,
            originDeviceId = "phone-1"
        )

        val packet = P2pPacket(
            type = P2pPacket.TYPE_CHANGES,
            senderDeviceId = "phone-1",
            senderDeviceName = "Phone 1",
            changes = listOf(changePayload)
        )

        val packetJson = serializer.toJson(packet)
        val restoredPacket = serializer.fromJson(packetJson, P2pPacket::class.java)
        assertEquals(1, restoredPacket.changes?.size)

        val restoredTxn = serializer.fromJson(restoredPacket.changes!![0].payloadJson, TransactionEntity::class.java)
        assertEquals("Whole Foods", restoredTxn.merchantName)
        assertEquals(BigDecimal("99.99"), restoredTxn.amount)
        assertEquals("Produce", restoredTxn.subcategory)
        assertEquals("uuid-wf-99", restoredTxn.uuid)
    }

    @Test
    fun testPairedDeviceDisplayNameAndRenaming() {
        val device = com.reddy.vittify.data.sync.model.PairedDevice(
            deviceId = "dev-123",
            deviceName = "Pixel 7 Pro"
        )
        assertEquals("Pixel 7 Pro", device.displayName)

        val renamed = device.copy(customNickname = "Living Room Phone")
        assertEquals("Living Room Phone", renamed.displayName)

        val packet = P2pPacket(
            type = P2pPacket.TYPE_DEVICE_RENAME,
            senderDeviceId = "dev-123",
            senderDeviceName = "Office Pixel"
        )
        val json = serializer.toJson(packet)
        val restored = serializer.fromJson(json, P2pPacket::class.java)
        assertEquals(P2pPacket.TYPE_DEVICE_RENAME, restored.type)
        assertEquals("Office Pixel", restored.senderDeviceName)
    }

    @Test
    fun testSyncRequestPacketSerialization() {
        val packet = P2pPacket(
            type = P2pPacket.TYPE_SYNC_REQUEST,
            senderDeviceId = "dev-abc",
            senderDeviceName = "Partner Phone"
        )
        val json = serializer.toJson(packet)
        val restored = serializer.fromJson(json, P2pPacket::class.java)
        assertEquals(P2pPacket.TYPE_SYNC_REQUEST, restored.type)
        assertEquals("dev-abc", restored.senderDeviceId)
    }

    @Test
    fun testCardAndSubscriptionPayloadSerialization() {
        val card = CardEntity(
            id = 12L,
            cardLast4 = "4321",
            cardType = CardType.CREDIT,
            bankName = "HDFC",
            nickname = "Regalia Gold",
            ownerId = "partner-dev"
        )
        val cardJson = serializer.toJson(card)
        val cardPayload = SyncChangePayload(
            entityType = SyncChangeEntity.TYPE_CARD,
            entityId = "${card.bankName}:${card.cardLast4}",
            operation = SyncChangeEntity.OP_INSERT,
            payloadJson = cardJson,
            timestamp = System.currentTimeMillis(),
            originDeviceId = "partner-dev"
        )
        val restoredCard = serializer.fromJson(cardPayload.payloadJson, CardEntity::class.java)
        assertEquals("HDFC", restoredCard.bankName)
        assertEquals("4321", restoredCard.cardLast4)
        assertEquals(CardType.CREDIT, restoredCard.cardType)

        val sub = SubscriptionEntity(
            id = 5L,
            merchantName = "Spotify",
            amount = BigDecimal("119.00"),
            nextPaymentDate = null,
            ownerId = "partner-dev"
        )
        val subJson = serializer.toJson(sub)
        val subPayload = SyncChangePayload(
            entityType = SyncChangeEntity.TYPE_SUBSCRIPTION,
            entityId = sub.id.toString(),
            operation = SyncChangeEntity.OP_INSERT,
            payloadJson = subJson,
            timestamp = System.currentTimeMillis(),
            originDeviceId = "partner-dev"
        )
        val restoredSub = serializer.fromJson(subPayload.payloadJson, SubscriptionEntity::class.java)
        assertEquals("Spotify", restoredSub.merchantName)
        assertEquals(BigDecimal("119.00"), restoredSub.amount)
    }

    @Test
    fun testWebRtcSignalingPacketSerializationAndDeserialization() {
        val signalingMessage = com.reddy.vittify.data.sync.transport.SignalingMessage(
            type = com.reddy.vittify.data.sync.transport.SignalingMessage.TYPE_OFFER,
            fromDeviceId = "dev-offer-1",
            fromDeviceName = "Pixel 8 Pro",
            toDeviceId = "dev-answer-2",
            clusterId = "vittify-cluster-123",
            sdp = "v=0\r\no=- 12345 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n",
            timestamp = 1725899999000L
        )

        val packet = P2pPacket(
            type = P2pPacket.TYPE_WEBRTC_SIGNALING,
            senderDeviceId = "dev-offer-1",
            senderDeviceName = "Pixel 8 Pro",
            timestamp = 1725899999000L,
            signalingMessage = signalingMessage
        )

        val json = serializer.toJson(packet)
        val restored = serializer.fromJson(json, P2pPacket::class.java)

        assertEquals(P2pPacket.TYPE_WEBRTC_SIGNALING, restored.type)
        assertEquals("dev-offer-1", restored.senderDeviceId)
        assertNotNull(restored.signalingMessage)
        assertEquals(com.reddy.vittify.data.sync.transport.SignalingMessage.TYPE_OFFER, restored.signalingMessage?.type)
        assertEquals("vittify-cluster-123", restored.signalingMessage?.clusterId)
        assertEquals("dev-answer-2", restored.signalingMessage?.toDeviceId)
        assertEquals("v=0\r\no=- 12345 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n", restored.signalingMessage?.sdp)
        assertNotNull(restored.signalingMessage?.messageId)
    }

    @Test
    fun testSignalingMessageEncryptionAndDecryptionRoundtrip() {
        val secret = "vittify-cluster-test-secret-999"
        val key = cryptoEngine.deriveKey(secret)

        val signalingMessage = com.reddy.vittify.data.sync.transport.SignalingMessage(
            type = com.reddy.vittify.data.sync.transport.SignalingMessage.TYPE_ICE_CANDIDATE,
            fromDeviceId = "dev-cand-1",
            fromDeviceName = "Galaxy S24",
            toDeviceId = "dev-target-2",
            clusterId = "vittify-cluster-test",
            candidateSdp = "candidate:1 1 UDP 2130706431 192.168.1.100 50000 typ host",
            candidateSdpMid = "data",
            candidateSdpMLineIndex = 0
        )

        val json = serializer.toJson(signalingMessage)
        val encryptedBase64 = cryptoEngine.encryptString(json, key)
        assertNotNull(encryptedBase64)

        val decryptedJson = cryptoEngine.decryptString(encryptedBase64, key)
        val restored = serializer.fromJson(decryptedJson, com.reddy.vittify.data.sync.transport.SignalingMessage::class.java)

        assertEquals(signalingMessage.type, restored.type)
        assertEquals(signalingMessage.fromDeviceId, restored.fromDeviceId)
        assertEquals(signalingMessage.candidateSdp, restored.candidateSdp)
        assertEquals(signalingMessage.candidateSdpMid, restored.candidateSdpMid)
        assertEquals(signalingMessage.candidateSdpMLineIndex, restored.candidateSdpMLineIndex)
        assertEquals(signalingMessage.messageId, restored.messageId)
    }

    @Test
    fun testLockstepSyncStartMetaPacketSerialization() {
        val meta = com.reddy.vittify.data.sync.model.SyncSessionMetaPayload(
            sessionId = "sess-12345",
            totalRecords = 1036,
            step = 1,
            isInitialPairSync = true
        )
        val packet = P2pPacket(
            type = P2pPacket.TYPE_SYNC_START_META,
            senderDeviceId = "phone-a",
            senderDeviceName = "Phone A",
            syncMeta = meta
        )

        val json = serializer.toJson(packet)
        val restored = serializer.fromJson(json, P2pPacket::class.java)

        assertEquals(P2pPacket.TYPE_SYNC_START_META, restored.type)
        assertEquals("phone-a", restored.senderDeviceId)
        assertNotNull(restored.syncMeta)
        assertEquals("sess-12345", restored.syncMeta?.sessionId)
        assertEquals(1036, restored.syncMeta?.totalRecords)
        assertEquals(1, restored.syncMeta?.step)
        assertTrue(restored.syncMeta?.isInitialPairSync == true)
    }

    @Test
    fun testLockstepSyncRecordPacketSerialization() {
        val changePayload = SyncChangePayload(
            entityType = SyncChangeEntity.TYPE_TRANSACTION,
            entityId = "txn-uuid-99",
            operation = SyncChangeEntity.OP_INSERT,
            payloadJson = "{\"amount\":120.5}",
            timestamp = 1725892500000L,
            originDeviceId = "phone-a"
        )
        val packet = P2pPacket(
            type = P2pPacket.TYPE_SYNC_RECORD,
            senderDeviceId = "phone-a",
            senderDeviceName = "Phone A",
            sequenceNumber = 42,
            totalRecords = 1036,
            singleChange = changePayload,
            step = 1
        )

        val json = serializer.toJson(packet)
        val restored = serializer.fromJson(json, P2pPacket::class.java)

        assertEquals(P2pPacket.TYPE_SYNC_RECORD, restored.type)
        assertEquals(42, restored.sequenceNumber)
        assertEquals(1036, restored.totalRecords)
        assertEquals(1, restored.step)
        assertNotNull(restored.singleChange)
        assertEquals("txn-uuid-99", restored.singleChange?.entityId)
    }

    @Test
    fun testLockstepSyncRecordAckPacketSerialization() {
        val recordIds = listOf(101L, 102L, 103L)
        val packet = P2pPacket(
            type = P2pPacket.TYPE_SYNC_RECORD_ACK,
            senderDeviceId = "phone-b",
            senderDeviceName = "Phone B",
            sequenceNumber = 42,
            step = 1,
            recordIds = recordIds
        )

        val json = serializer.toJson(packet)
        val restored = serializer.fromJson(json, P2pPacket::class.java)

        assertEquals(P2pPacket.TYPE_SYNC_RECORD_ACK, restored.type)
        assertEquals(42, restored.sequenceNumber)
        assertEquals(1, restored.step)
        assertEquals(recordIds, restored.recordIds)
    }

    @Test
    fun testSyncChangePayloadWithChangeIdSerialization() {
        val payload = SyncChangePayload(
            entityType = SyncChangeEntity.TYPE_TRANSACTION,
            entityId = "txn-123",
            operation = SyncChangeEntity.OP_INSERT,
            payloadJson = "{}",
            timestamp = 1725890000000L,
            originDeviceId = "dev-1",
            changeId = 8842L
        )

        val json = serializer.toJson(payload)
        val restored = serializer.fromJson(json, SyncChangePayload::class.java)

        assertEquals(8842L, restored.changeId)
        assertEquals("txn-123", restored.entityId)
    }

    @Test
    fun testSymmetricTurnTakingDecisionLogic() {
        // Case 1: Phone A is scanner, Phone B is not
        val phoneAIsScanner = true
        val phoneBIsScanner = false
        val deviceIdA = "device-xyz"
        val deviceIdB = "device-abc"

        val shouldPhoneAInitiate = when {
            phoneAIsScanner && !phoneBIsScanner -> true
            !phoneAIsScanner && phoneBIsScanner -> false
            else -> deviceIdA < deviceIdB
        }
        val shouldPhoneBInitiate = when {
            phoneBIsScanner && !phoneAIsScanner -> true
            !phoneBIsScanner && phoneAIsScanner -> false
            else -> deviceIdB < deviceIdA
        }
        assertTrue("Scanner device must initiate Step 1", shouldPhoneAInitiate)
        assertFalse("Non-scanner partner must NOT initiate Step 1", shouldPhoneBInitiate)

        // Case 2: Neither is scanner (routine sync) - tie-breaker by device ID
        val phoneAIsScanner2 = false
        val phoneBIsScanner2 = false
        val shouldA2Initiate = when {
            phoneAIsScanner2 && !phoneBIsScanner2 -> true
            !phoneAIsScanner2 && phoneBIsScanner2 -> false
            else -> deviceIdA < deviceIdB
        }
        val shouldB2Initiate = when {
            phoneBIsScanner2 && !phoneAIsScanner2 -> true
            !phoneBIsScanner2 && phoneAIsScanner2 -> false
            else -> deviceIdB < deviceIdA
        }
        assertFalse("Device with higher ID should not initiate", shouldA2Initiate)
        assertTrue("Device with lower ID must initiate", shouldB2Initiate)
    }

    @Test
    fun testHandshakeWithScannerFlagSerialization() {
        val handshake = HandshakePayload(
            lastSyncTimestamp = 0L,
            schemaVersion = 64,
            isScanner = true
        )
        val packet = P2pPacket(
            type = P2pPacket.TYPE_HANDSHAKE,
            senderDeviceId = "scanner-phone",
            senderDeviceName = "Scanner Phone",
            handshake = handshake
        )

        val json = serializer.toJson(packet)
        val restored = serializer.fromJson(json, P2pPacket::class.java)

        assertEquals(P2pPacket.TYPE_HANDSHAKE, restored.type)
        assertNotNull(restored.handshake)
        assertTrue(restored.handshake?.isScanner == true)
    }

    @Test
    fun testUnpairPacketSerialization() {
        val packet = P2pPacket(
            type = P2pPacket.TYPE_UNPAIR,
            senderDeviceId = "device-1",
            senderDeviceName = "Device 1"
        )
        val json = serializer.toJson(packet)
        val restored = serializer.fromJson(json, P2pPacket::class.java)
        assertEquals(P2pPacket.TYPE_UNPAIR, restored.type)
        assertEquals("device-1", restored.senderDeviceId)
    }

    @Test
    fun testSubcategorySyncPayloadSerialization() {
        val payload = com.reddy.vittify.data.sync.model.SubcategorySyncPayload(
            categoryName = "Food & Dining",
            name = "Coffee",
            iconResId = 123,
            iconName = "coffee_icon",
            color = "#FF9800",
            isSystem = true
        )
        val json = serializer.toJson(payload)
        val restored = serializer.fromJson(json, com.reddy.vittify.data.sync.model.SubcategorySyncPayload::class.java)
        assertEquals("Food & Dining", restored.categoryName)
        assertEquals("Coffee", restored.name)
        assertEquals(123, restored.iconResId)
        assertEquals("coffee_icon", restored.iconName)
        assertEquals("#FF9800", restored.color)
        assertTrue(restored.isSystem)
    }
}

