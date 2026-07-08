package com.kyrx.mypresence.core.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PKCE {
    private const val CODE_VERIFIER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    private const val VERIFIER_LENGTH = 64
    private val secureRandom = SecureRandom()

    fun generateCodeVerifier(): String {
        val chars = CharArray(VERIFIER_LENGTH)
        for (i in 0 until VERIFIER_LENGTH) {
            chars[i] = CODE_VERIFIER_CHARS[secureRandom.nextInt(CODE_VERIFIER_CHARS.length)]
        }
        return String(chars)
    }

    fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun generateState(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
