package org.mozilla.rocket.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.experimental.and

fun String.sha256(): String {
    fun convertToHex(data: ByteArray): String {
        val hex = StringBuilder(data.size * 2)
        for (byte in data) {
            hex.append(String.format("%02x", byte and 0xFF.toByte()))
        }
        return hex.toString()
    }

    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedByteArray = digest.digest(this.toByteArray(StandardCharsets.UTF_8))
        convertToHex(hashedByteArray)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}