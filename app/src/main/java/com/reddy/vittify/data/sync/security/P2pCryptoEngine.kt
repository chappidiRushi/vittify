package com.reddy.vittify.data.sync.security

import java.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * End-to-End Encryption engine for P2P sync and signaling payloads.
 * Uses AES-256-GCM with randomized 12-byte IVs and PBKDF2-HMAC-SHA256 key derivation.
 */
@Singleton
class P2pCryptoEngine @Inject constructor() {

    companion object {
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PBKDF2_ITERATIONS = 10_000
        private val FIXED_SALT = "vittify-p2p-cluster-salt-2026".toByteArray(Charsets.UTF_8)
    }

    private val secureRandom = SecureRandom()

    /**
     * Derives a 256-bit AES key from a passphrase or cluster code.
     */
    fun deriveKey(secret: String): SecretKey {
        val spec = PBEKeySpec(secret.toCharArray(), FIXED_SALT, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts raw bytes. Returns: [12-byte IV] + [Ciphertext + Auth Tag]
     */
    fun encrypt(plaintext: ByteArray, key: SecretKey): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val cipherBytes = cipher.doFinal(plaintext)

        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return combined
    }

    /**
     * Decrypts byte array that starts with a 12-byte IV.
     */
    fun decrypt(encryptedWithIv: ByteArray, key: SecretKey): ByteArray {
        require(encryptedWithIv.size > GCM_IV_LENGTH) { "Ciphertext too short" }
        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH)

        val cipherBytes = ByteArray(encryptedWithIv.size - GCM_IV_LENGTH)
        System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(cipherBytes)
    }

    /**
     * Encrypts a string to a Base64-encoded string.
     */
    fun encryptString(plaintext: String, key: SecretKey): String {
        val encrypted = encrypt(plaintext.toByteArray(Charsets.UTF_8), key)
        return Base64.getEncoder().encodeToString(encrypted)
    }

    /**
     * Decrypts a Base64-encoded string.
     */
    fun decryptString(base64Cipher: String, key: SecretKey): String {
        val bytes = Base64.getDecoder().decode(base64Cipher)
        val decrypted = decrypt(bytes, key)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * Converts a secret key to Base64 for pairing representation.
     */
    fun keyToBase64(key: SecretKey): String {
        return Base64.getEncoder().encodeToString(key.encoded)
    }

    /**
     * Recreates a SecretKey from Base64.
     */
    fun keyFromBase64(base64Key: String): SecretKey {
        val bytes = Base64.getDecoder().decode(base64Key)
        return SecretKeySpec(bytes, "AES")
    }
}

