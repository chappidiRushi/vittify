package com.reddy.vittify.utils

import java.security.SecureRandom
import java.util.Random

/**
 * Lightweight, collision-resistant NanoID generator for Vittify entity identifiers.
 */
object NanoId {
    private const val ALPHABET = "_~0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DEFAULT_SIZE = 21

    fun generate(random: Random = SecureRandom(), size: Int = DEFAULT_SIZE): String {
        val builder = StringBuilder(size)
        for (i in 0 until size) {
            builder.append(ALPHABET[random.nextInt(ALPHABET.length)])
        }
        return builder.toString()
    }
}
