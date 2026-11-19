package com.example.data

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {
    private const val ITERATIONS = 3

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var current = "$salt:$password".toByteArray(Charsets.UTF_8)
        repeat(ITERATIONS) {
            current = digest.digest(current)
        }
        return current.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        return hashPassword(password, salt) == expectedHash
    }

    fun generateRecoveryCode(): String {
        val code = 100000 + SecureRandom().nextInt(900000)
        return code.toString()
    }
}
